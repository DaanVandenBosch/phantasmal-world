package world.phantasmal.web.questEditor.controllers

import kotlinx.browser.document
import mu.KotlinLogging
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import world.phantasmal.core.*
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.Episode
import world.phantasmal.lib.fileFormats.quest.*
import world.phantasmal.observable.value.*
import world.phantasmal.observable.value.list.MutableListVal
import world.phantasmal.observable.value.list.mutableListVal
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.files.cursor
import world.phantasmal.web.core.files.writeBuffer
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.questEditor.models.AreaModel
import world.phantasmal.web.questEditor.stores.AreaStore
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.web.questEditor.stores.convertQuestFromModel
import world.phantasmal.web.questEditor.stores.convertQuestToModel
import world.phantasmal.webui.BrowserFeatures
import world.phantasmal.webui.controllers.Controller
import world.phantasmal.webui.files.FileHandle
import world.phantasmal.webui.files.FileType
import world.phantasmal.webui.files.showFilePicker
import world.phantasmal.webui.obj

private val logger = KotlinLogging.logger {}

class AreaAndLabel(val area: AreaModel, val label: String)

class QuestEditorToolbarController(
    uiStore: UiStore,
    private val areaStore: AreaStore,
    private val questEditorStore: QuestEditorStore,
) : Controller() {
    private val _resultDialogVisible = mutableVal(false)
    private val _result = mutableVal<PwResult<*>?>(null)
    private val _saveAsDialogVisible = mutableVal(false)
    private val saving = mutableVal(false)

    // We mainly disable saving while a save is underway for visual feedback that a save is
    // happening/has happened.
    private val savingEnabled = questEditorStore.currentQuest.isNotNull() and !saving
    private val files: MutableListVal<FileHandle> = mutableListVal()
    private val _filename = mutableVal("")
    private val _version = mutableVal(Version.BB)

    // Result

    val resultDialogVisible: Val<Boolean> = _resultDialogVisible
    val result: Val<PwResult<*>?> = _result

    val supportedFileTypes = listOf(
        FileType(
            description = "Quests",
            accept = mapOf("application/pw-quest" to setOf(".qst", ".bin", ".dat")),
        ),
    )

    // Save as

    val saveEnabled: Val<Boolean> =
        savingEnabled and files.notEmpty and BrowserFeatures.fileSystemApi
    val saveTooltip: Val<String> = value(
        if (BrowserFeatures.fileSystemApi) "Save changes (Ctrl-S)"
        else "This browser doesn't support saving to an existing file"
    )
    val saveAsEnabled: Val<Boolean> = savingEnabled
    val saveAsDialogVisible: Val<Boolean> = _saveAsDialogVisible
    val filename: Val<String> = _filename
    val version: Val<Version> = _version

    // Undo

    val undoTooltip: Val<String> = questEditorStore.firstUndo.map { action ->
        (action?.let { "Undo \"${action.description}\"" } ?: "Nothing to undo") + " (Ctrl-Z)"
    }

    val undoEnabled: Val<Boolean> = questEditorStore.canUndo

    // Redo

    val redoTooltip: Val<String> = questEditorStore.firstRedo.map { action ->
        (action?.let { "Redo \"${action.description}\"" } ?: "Nothing to redo") + " (Ctrl-Shift-Z)"
    }

    val redoEnabled: Val<Boolean> = questEditorStore.canRedo

    // Areas

    // Ensure the areas list is updated when entities are added or removed (the count in the label
    // should update).
    val areas: Val<List<AreaAndLabel>> = questEditorStore.currentQuest.flatMap { quest ->
        quest?.let {
            map(quest.entitiesPerArea, quest.areaVariants) { entitiesPerArea, variants ->
                areaStore.getAreasForEpisode(quest.episode).map { area ->
                    val entityCount = entitiesPerArea[area.id]
                    val name = variants.firstOrNull { it.area == area }?.name ?: area.name
                    AreaAndLabel(area, name + (entityCount?.let { " ($it)" } ?: ""))
                }
            }
        } ?: value(emptyList())
    }

    val currentArea: Val<AreaAndLabel?> = map(areas, questEditorStore.currentArea) { areas, area ->
        areas.find { it.area == area }
    }

    val areaSelectEnabled: Val<Boolean> = questEditorStore.currentQuest.isNotNull()

    // Settings

    val showCollisionGeometry: Val<Boolean> = questEditorStore.showCollisionGeometry

    init {
        addDisposables(
            uiStore.onGlobalKeyDown(PwToolType.QuestEditor, "Ctrl-O") {
                openFiles(showFilePicker(supportedFileTypes, multiple = true))
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
        setFilename("")
        setVersion(Version.BB)
        questEditorStore.setDefaultQuest(episode)
    }

    suspend fun openFiles(newFiles: List<FileHandle>?) {
        try {
            files.clear()

            if (newFiles.isNullOrEmpty()) return

            val qstFile = newFiles.find { it.extension().equals("qst", ignoreCase = true) }

            if (qstFile != null) {
                val parseResult = parseQstToQuest(qstFile.cursor(Endianness.Little))
                setResult(parseResult)

                if (parseResult is Success) {
                    setFilename(filenameBase(qstFile.name) ?: qstFile.name)
                    setVersion(parseResult.value.version)
                    setCurrentQuest(parseResult.value.quest)
                    files.replaceAll(listOf(qstFile))
                }
            } else {
                val binFile = newFiles.find { it.extension().equals("bin", ignoreCase = true) }
                val datFile = newFiles.find { it.extension().equals("dat", ignoreCase = true) }

                if (binFile == null || datFile == null) {
                    setResult(Failure(listOf(Problem(
                        Severity.Error,
                        "Please select a .qst file or one .bin and one .dat file.",
                    ))))
                    return
                }

                val parseResult = parseBinDatToQuest(
                    binFile.cursor(Endianness.Little),
                    datFile.cursor(Endianness.Little),
                )
                setResult(parseResult)

                if (parseResult is Success) {
                    setFilename(binFile.basename() ?: datFile.basename() ?: binFile.name)
                    setVersion(Version.BB)
                    setCurrentQuest(parseResult.value)
                    files.replaceAll(listOf(binFile, datFile))
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
            val files = files.value.filterIsInstance<FileHandle.Fsaa>()

            files.find { it.extension().equals("qst", ignoreCase = true) }?.let { qstFile ->
                val buffer = writeQuestToQst(
                    convertQuestFromModel(quest),
                    headerFilename,
                    version.value,
                    online = true,
                )

                qstFile.writeBuffer(buffer)
            }

            val binFile = files.find { it.extension().equals("bin", ignoreCase = true) }
            val datFile = files.find { it.extension().equals("dat", ignoreCase = true) }

            if (binFile != null && datFile != null) {
                val (bin, dat) = writeQuestToBinDat(
                    convertQuestFromModel(quest),
                    version.value,
                )

                binFile.writeBuffer(bin)
                datFile.writeBuffer(dat)
            }
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

    fun saveAsDialogSave() {
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

            val a = document.createElement("a") as HTMLAnchorElement
            val url = URL.createObjectURL(
                Blob(
                    arrayOf(buffer.arrayBuffer),
                    obj { type = "application/octet-stream" },
                )
            )

            try {
                a.href = url
                a.download = filename
                document.body?.appendChild(a)
                a.click()
            } finally {
                URL.revokeObjectURL(url)
                document.body?.removeChild(a)
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

    private suspend fun setCurrentQuest(quest: Quest) {
        questEditorStore.setCurrentQuest(convertQuestToModel(quest, areaStore::getVariant))
    }

    private fun setResult(result: PwResult<*>) {
        _result.value = result

        if (result.problems.isNotEmpty()) {
            _resultDialogVisible.value = true
        }
    }
}
