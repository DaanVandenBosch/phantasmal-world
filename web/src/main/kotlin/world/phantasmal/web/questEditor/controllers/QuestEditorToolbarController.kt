package world.phantasmal.web.questEditor.controllers

import kotlinx.browser.document
import mu.KotlinLogging
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.File
import world.phantasmal.core.*
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.Episode
import world.phantasmal.lib.cursor.cursor
import world.phantasmal.lib.fileFormats.quest.*
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.map
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.observable.value.value
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.questEditor.loading.QuestLoader
import world.phantasmal.web.questEditor.models.AreaModel
import world.phantasmal.web.questEditor.stores.AreaStore
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.web.questEditor.stores.convertQuestFromModel
import world.phantasmal.web.questEditor.stores.convertQuestToModel
import world.phantasmal.webui.controllers.Controller
import world.phantasmal.webui.obj
import world.phantasmal.webui.readFile
import world.phantasmal.webui.selectFiles

private val logger = KotlinLogging.logger {}

class AreaAndLabel(val area: AreaModel, val label: String)

class QuestEditorToolbarController(
    uiStore: UiStore,
    private val questLoader: QuestLoader,
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

    val openFileAccept = ".bin, .dat, .qst"

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
            quest.entitiesPerArea.map { entitiesPerArea ->
                areaStore.getAreasForEpisode(quest.episode).map { area ->
                    val entityCount = entitiesPerArea[area.id]
                    AreaAndLabel(area, area.name + (entityCount?.let { " ($it)" } ?: ""))
                }
            }
        } ?: value(emptyList())
    }

    val currentArea: Val<AreaAndLabel?> = map(areas, questEditorStore.currentArea) { areas, area ->
        areas.find { it.area == area }
    }

    val areaSelectEnabled: Val<Boolean> = questEditorStore.currentQuest.isNotNull()

    init {
        addDisposables(
            uiStore.onGlobalKeyDown(PwToolType.QuestEditor, "Ctrl-O") {
                openFiles(selectFiles(accept = openFileAccept, multiple = true))
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
        setCurrentQuest(questLoader.loadDefaultQuest(episode))
    }

    suspend fun openFiles(files: List<File>) {
        try {
            if (files.isEmpty()) return

            val qst = files.find { it.name.endsWith(".qst", ignoreCase = true) }

            if (qst != null) {
                val parseResult = parseQstToQuest(readFile(qst).cursor(Endianness.Little))
                setResult(parseResult)

                if (parseResult is Success) {
                    setFilename(filenameBase(qst.name) ?: qst.name)
                    setVersion(parseResult.value.version)
                    setCurrentQuest(parseResult.value.quest)
                }
            } else {
                val bin = files.find { it.name.endsWith(".bin", ignoreCase = true) }
                val dat = files.find { it.name.endsWith(".dat", ignoreCase = true) }

                if (bin == null || dat == null) {
                    setResult(Failure(listOf(Problem(
                        Severity.Error,
                        "Please select a .qst file or one .bin and one .dat file."
                    ))))
                    return
                }

                val parseResult = parseBinDatToQuest(
                    readFile(bin).cursor(Endianness.Little),
                    readFile(dat).cursor(Endianness.Little),
                )
                setResult(parseResult)

                if (parseResult is Success) {
                    setFilename(filenameBase(bin.name) ?: filenameBase(dat.name) ?: bin.name)
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
