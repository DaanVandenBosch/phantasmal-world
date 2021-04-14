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
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.map
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.observable.value.value
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.files.cursor
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.questEditor.models.AreaModel
import world.phantasmal.web.questEditor.stores.AreaStore
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.web.questEditor.stores.convertQuestFromModel
import world.phantasmal.web.questEditor.stores.convertQuestToModel
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

    val saveAsEnabled: Val<Boolean> = questEditorStore.currentQuest.isNotNull()
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

    suspend fun openFiles(files: List<FileHandle>?) {
        try {
            if (files.isNullOrEmpty()) return

            val qstFile = files.find { it.extension().equals("qst", ignoreCase = true) }

            if (qstFile != null) {
                val parseResult = parseQstToQuest(qstFile.cursor(Endianness.Little))
                setResult(parseResult)

                if (parseResult is Success) {
                    setFilename(filenameBase(qstFile.name) ?: qstFile.name)
                    setVersion(parseResult.value.version)
                    setCurrentQuest(parseResult.value.quest)
                }
            } else {
                val binFile = files.find { it.extension().equals("bin", ignoreCase = true) }
                val datFile = files.find { it.extension().equals("dat", ignoreCase = true) }

                if (binFile == null || datFile == null) {
                    setResult(Failure(listOf(Problem(
                        Severity.Error,
                        "Please select a .qst file or one .bin and one .dat file."
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
                }
            }
        } catch (e: Exception) {
            setResult(
                PwResult.build<Nothing>(logger)
                    .addProblem(Severity.Error, "Couldn't parse file.", cause = e)
                    .failure()
            )
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
        val quest = questEditorStore.currentQuest.value ?: return
        var filename = filename.value.trim()

        val buffer = writeQuestToQst(
            convertQuestFromModel(quest),
            filename,
            version.value,
            online = true,
        )

        if (!filename.endsWith(".qst")) {
            filename += ".qst"
        }

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
        } catch (e: Exception) {
            logger.error(e) { """Couldn't save file "$filename".""" }
        } finally {
            URL.revokeObjectURL(url)
            document.body?.removeChild(a)
        }

        dismissSaveAsDialog()
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
