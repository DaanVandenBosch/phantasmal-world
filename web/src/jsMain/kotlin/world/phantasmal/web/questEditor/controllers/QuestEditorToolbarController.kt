package world.phantasmal.web.questEditor.controllers

import kotlinx.coroutines.await
import mu.KotlinLogging
import world.phantasmal.cell.*
import world.phantasmal.core.*
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.fileFormats.quest.*
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.files.cursor
import world.phantasmal.web.core.files.writeBuffer
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.questEditor.models.AreaModel
import world.phantasmal.web.questEditor.models.QuestModel
import world.phantasmal.web.questEditor.stores.AreaStore
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.web.questEditor.stores.convertQuestFromModel
import world.phantasmal.web.questEditor.stores.convertQuestToModel
import world.phantasmal.webui.UserAgentFeatures
import world.phantasmal.webui.controllers.Controller
import world.phantasmal.webui.files.*

private val logger = KotlinLogging.logger {}

class AreaAndLabel(val area: AreaModel, val label: String)

class QuestEditorToolbarController(
    uiStore: UiStore,
    private val areaStore: AreaStore,
    private val questEditorStore: QuestEditorStore,
) : Controller() {
    private val _resultDialogVisible = mutableCell(false)
    private val _result = mutableCell<PwResult<*>?>(null)
    private val saving = mutableCell(false)

    // We mainly disable saving while a save is underway for visual feedback that a save is
    // happening/has happened.
    private val savingEnabled = questEditorStore.currentQuest.isNotNull() and !saving
    private val _saveAsDialogVisible = mutableCell(false)
    private val fileHolder = mutableCell<FileHolder?>(null)
    private val _filename = mutableCell("")
    private val _version = mutableCell(Version.BB)

    // Result

    val resultDialogVisible: Cell<Boolean> = _resultDialogVisible
    val result: Cell<PwResult<*>?> = _result

    val supportedFileTypes = listOf(
        FileType(
            description = "Quests",
            accept = mapOf("application/pw-quest" to setOf(".qst", ".bin", ".dat")),
        ),
    )

    // Saving

    val saveEnabled: Cell<Boolean> =
        savingEnabled and questEditorStore.canSaveChanges and UserAgentFeatures.fileSystemApi
    val saveTooltip: Cell<String> =
        if (UserAgentFeatures.fileSystemApi) {
            questEditorStore.canSaveChanges.map {
                (if (it) "Save changes" else "No changes to save") + " (Ctrl-S)"
            }
        } else {
            cell("This browser doesn't support saving changes to existing files")
        }
    val saveAsEnabled: Cell<Boolean> = savingEnabled
    val saveAsDialogVisible: Cell<Boolean> = _saveAsDialogVisible
    val showSaveAsDialogNameField: Boolean = !UserAgentFeatures.fileSystemApi
    val filename: Cell<String> = _filename
    val version: Cell<Version> = _version

    // Undo

    val undoTooltip: Cell<String> = questEditorStore.firstUndo.map { command ->
        (command?.let { "Undo \"${command.description}\"" } ?: "Nothing to undo") + " (Ctrl-Z)"
    }

    val undoEnabled: Cell<Boolean> = questEditorStore.canUndo

    // Redo

    val redoTooltip: Cell<String> = questEditorStore.firstRedo.map { command ->
        (command?.let { "Redo \"${command.description}\"" } ?: "Nothing to redo") +
                " (Ctrl-Shift-Z)"
    }

    val redoEnabled: Cell<Boolean> = questEditorStore.canRedo

    // Areas

    // Ensure the areas list is updated when entities are added or removed (the count in the label
    // should update).
    val areas: Cell<List<AreaAndLabel>> = questEditorStore.currentQuest.flatMap { quest ->
        quest?.let {
            map(quest.entitiesPerArea, quest.areaVariants) { entitiesPerArea, variants ->
                areaStore.getAreasForEpisode(quest.episode).map { area ->
                    val entityCount = entitiesPerArea[area.id]
                    val name = variants.firstOrNull { it.area == area }?.name ?: area.name
                    AreaAndLabel(area, name + (entityCount?.let { " ($it)" } ?: ""))
                }
            }
        } ?: cell(emptyList())
    }

    val currentArea: Cell<AreaAndLabel?> = map(areas, questEditorStore.currentArea) { areas, area ->
        areas.find { it.area == area }
    }

    val areaSelectEnabled: Cell<Boolean> = questEditorStore.currentQuest.isNotNull()

    // Settings

    val showCollisionGeometry: Cell<Boolean> = questEditorStore.showCollisionGeometry

    init {
        addDisposables(
            uiStore.onGlobalKeyDown(PwToolType.QuestEditor, "Ctrl-O") {
                openFiles(showOpenFilePicker(supportedFileTypes, multiple = true))
            },

            uiStore.onGlobalKeyDown(PwToolType.QuestEditor, "Ctrl-S") {
                save()
            },

            uiStore.onGlobalKeyDown(PwToolType.QuestEditor, "Ctrl-Shift-S") {
                saveAs()
            },

            uiStore.onGlobalKeyDown(PwToolType.QuestEditor, "Ctrl-Z") {
                undo()
            },

            uiStore.onGlobalKeyDown(PwToolType.QuestEditor, "Ctrl-Shift-Z") {
                redo()
            },

            uiStore.onGlobalKeyDown(PwToolType.QuestEditor, "Ctrl-Y") {
                redo()
            },
        )
    }

    suspend fun createNewQuest(episode: Episode) {
        setCurrentQuest(fileHolder = null, Version.BB, questEditorStore.getDefaultQuest(episode))
    }

    suspend fun openFiles(newFiles: List<FileHandle>?) {
        try {
            if (newFiles.isNullOrEmpty()) return

            val qstFile = newFiles.find { it.extension().equals("qst", ignoreCase = true) }

            if (qstFile != null) {
                val parseResult = parseQstToQuest(qstFile.cursor(Endianness.Little))
                setResult(parseResult)

                if (parseResult is Success) {
                    setCurrentQuest(
                        FileHolder.Qst(qstFile),
                        parseResult.value.version,
                        parseResult.value.quest,
                    )
                }
            } else {
                val binFile = newFiles.find { it.extension().equals("bin", ignoreCase = true) }
                val datFile = newFiles.find { it.extension().equals("dat", ignoreCase = true) }

                if (binFile == null || datFile == null) {
                    setResult(
                        Failure(
                            listOf(
                                Problem(
                                    Severity.Error,
                                    "Please select a .qst file or one .bin and one .dat file.",
                                )
                            )
                        )
                    )
                    return
                }

                val parseResult = parseBinDatToQuest(
                    binFile.cursor(Endianness.Little),
                    datFile.cursor(Endianness.Little),
                )
                setResult(parseResult)

                if (parseResult is Success) {
                    setCurrentQuest(
                        FileHolder.BinDat(binFile, datFile),
                        Version.BB,
                        parseResult.value,
                    )
                }
            }
        } catch (e: Throwable) {
            setResult(
                PwResult.build<Nothing>(logger)
                    .addProblem(Severity.Error, "Couldn't parse file.", cause = e)
                    .failure()
            )
        }
    }

    suspend fun save() {
        if (!saveEnabled.value) return

        try {
            saving.value = true

            val quest = questEditorStore.currentQuest.value ?: return
            val headerFilename = filename.value.trim()

            when (val holder = fileHolder.value) {
                is FileHolder.Qst -> {
                    if (holder.file is FileHandle.System) {
                        val buffer = writeQuestToQst(
                            convertQuestFromModel(quest),
                            headerFilename,
                            version.value,
                            online = true,
                        )

                        holder.file.writeBuffer(buffer)

                        questEditorStore.questSaved()
                        return
                    }
                }

                is FileHolder.BinDat -> {
                    if (holder.binFile is FileHandle.System &&
                        holder.datFile is FileHandle.System
                    ) {
                        val (bin, dat) = writeQuestToBinDat(
                            convertQuestFromModel(quest),
                            version.value,
                        )

                        holder.binFile.writeBuffer(bin)
                        holder.datFile.writeBuffer(dat)

                        questEditorStore.questSaved()
                        return
                    }
                }

                else -> {}
            }

            // When there's no existing file that can be saved, default to "Save as...".
            _saveAsDialogVisible.value = true
        } catch (e: Throwable) {
            setResult(
                PwResult.build<Nothing>(logger)
                    .addProblem(Severity.Error, "Couldn't save file.", cause = e)
                    .failure()
            )
        } finally {
            saving.value = false
        }
    }

    fun saveAs() {
        if (saveAsEnabled.value) {
            _saveAsDialogVisible.value = true
        }
    }

    fun setFilename(filename: String) {
        _filename.value = filename
    }

    fun setVersion(version: Version) {
        _version.value = version
    }

    suspend fun saveAsDialogSave() {
        if (!saveAsEnabled.value) return

        val quest = questEditorStore.currentQuest.value ?: return

        try {
            saving.value = true

            val headerFilename = filename.value.trim()
            val filename =
                if (headerFilename.endsWith(".qst")) headerFilename
                else "$headerFilename.qst"

            val buffer = writeQuestToQst(
                convertQuestFromModel(quest),
                headerFilename,
                version.value,
                online = true,
            )

            if (UserAgentFeatures.fileSystemApi) {
                val fileHandle = showSaveFilePicker(
                    listOf(
                        FileType("Quest file", mapOf("application/pw-quest" to setOf(".qst")))
                    )
                )

                if (fileHandle != null) {
                    fileHandle.writableStream().use { it.write(buffer.arrayBuffer).await() }

                    setFileHolder(FileHolder.Qst(fileHandle))
                    questEditorStore.questSaved()
                }
            } else {
                val fileHandle = downloadFile(buffer.arrayBuffer, filename)
                setFileHolder(FileHolder.Qst(fileHandle))
                questEditorStore.questSaved()
            }
        } catch (e: Throwable) {
            setResult(
                PwResult.build<Nothing>(logger)
                    .addProblem(Severity.Error, "Couldn't save file.", cause = e)
                    .failure()
            )
        } finally {
            dismissSaveAsDialog()
            saving.value = false
        }
    }

    fun dismissSaveAsDialog() {
        _saveAsDialogVisible.value = false
    }

    fun dismissResultDialog() {
        _resultDialogVisible.value = false
    }

    fun undo() {
        questEditorStore.undo()
    }

    fun redo() {
        questEditorStore.redo()
    }

    fun setCurrentArea(areaAndLabel: AreaAndLabel) {
        questEditorStore.setCurrentArea(areaAndLabel.area)
    }

    fun setShowCollisionGeometry(show: Boolean) {
        questEditorStore.setShowCollisionGeometry(show)
    }

    private fun setFileHolder(fileHolder: FileHolder?) {
        setFilename(
            when (fileHolder) {
                is FileHolder.Qst -> fileHolder.file.basename() ?: fileHolder.file.name

                is FileHolder.BinDat ->
                    fileHolder.binFile.basename()
                        ?: fileHolder.datFile.basename()
                        ?: fileHolder.binFile.name

                null -> ""
            }
        )
        this.fileHolder.value = fileHolder
    }

    private suspend fun setCurrentQuest(
        fileHolder: FileHolder?,
        version: Version,
        quest: QuestModel,
    ) {
        setFileHolder(fileHolder)
        setVersion(version)
        questEditorStore.setCurrentQuest(quest)
    }

    private suspend fun setCurrentQuest(
        fileHolder: FileHolder?,
        version: Version,
        quest: Quest,
    ) {
        setCurrentQuest(fileHolder, version, convertQuestToModel(quest, areaStore::getVariant))
    }

    private fun setResult(result: PwResult<*>) {
        _result.value = result

        if (result.problems.isNotEmpty()) {
            _resultDialogVisible.value = true
        }
    }

    private sealed class FileHolder {
        class Qst(val file: FileHandle) : FileHolder()
        class BinDat(val binFile: FileHandle, val datFile: FileHandle) : FileHolder()
    }
}
