package world.phantasmal.web.questEditor.controllers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.w3c.files.File
import world.phantasmal.core.*
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.cursor.ArrayBufferCursor
import world.phantasmal.lib.fileFormats.quest.Quest
import world.phantasmal.lib.fileFormats.quest.parseBinDatToQuest
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.web.questEditor.stores.convertQuestToModel
import world.phantasmal.webui.controllers.Controller
import world.phantasmal.webui.readFile

class QuestEditorToolbarController(
    scope: CoroutineScope,
    private val questEditorStore: QuestEditorStore
) : Controller(scope) {
    private val _resultDialogVisible = mutableVal(false)
    private val _result = mutableVal<PwResult<*>?>(null)

    val resultDialogVisible: Val<Boolean> = _resultDialogVisible
    val result: Val<PwResult<*>?> = _result

    fun openFiles(files: List<File>) {
        launch {
            if (files.isEmpty()) return@launch

            val qst = files.find { it.name.endsWith(".qst", ignoreCase = true) }

            if (qst != null) {
                val buffer = readFile(qst)
                // TODO: Parse qst.
            } else {
                val bin = files.find { it.name.endsWith(".bin", ignoreCase = true) }
                val dat = files.find { it.name.endsWith(".dat", ignoreCase = true) }

                if (bin == null || dat == null) {
                    setResult(Failure(listOf(Problem(
                        Severity.Error,
                        "Please select a .qst file or one .bin and one .dat file."
                    ))))
                    return@launch
                }

                val binBuffer = readFile(bin)
                val datBuffer = readFile(dat)
                val parseResult = parseBinDatToQuest(
                    ArrayBufferCursor(binBuffer, Endianness.Little),
                    ArrayBufferCursor(datBuffer, Endianness.Little)
                )
                setResult(parseResult)

                if (parseResult is Success) {
                    setCurrentQuest(parseResult.value)
                }
            }
        }
    }

    private fun setCurrentQuest(quest: Quest) {
        questEditorStore.setCurrentQuest(convertQuestToModel(quest))
    }

    private fun setResult(result: PwResult<*>) {
        _result.value = result

        if (result.problems.isNotEmpty()) {
            _resultDialogVisible.value = true
        }
    }
}
