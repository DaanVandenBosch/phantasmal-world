package world.phantasmal.web.questEditor.controllers

import kotlinx.coroutines.CoroutineScope
import mu.KotlinLogging
import org.w3c.files.File
import world.phantasmal.core.*
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.cursor.cursor
import world.phantasmal.lib.fileFormats.quest.Episode
import world.phantasmal.lib.fileFormats.quest.Quest
import world.phantasmal.lib.fileFormats.quest.parseBinDatToQuest
import world.phantasmal.lib.fileFormats.quest.parseQstToQuest
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.web.questEditor.loading.QuestLoader
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.web.questEditor.stores.convertQuestToModel
import world.phantasmal.webui.controllers.Controller
import world.phantasmal.webui.readFile

private val logger = KotlinLogging.logger {}

class QuestEditorToolbarController(
    scope: CoroutineScope,
    private val questLoader: QuestLoader,
    private val questEditorStore: QuestEditorStore,
) : Controller(scope) {
    private val _resultDialogVisible = mutableVal(false)
    private val _result = mutableVal<PwResult<*>?>(null)

    val resultDialogVisible: Val<Boolean> = _resultDialogVisible
    val result: Val<PwResult<*>?> = _result

    suspend fun createNewQuest(episode: Episode) {
        questEditorStore.setCurrentQuest(
            convertQuestToModel(questLoader.loadDefaultQuest(episode))
        )
    }

    suspend fun openFiles(files: List<File>) {
        try {
            if (files.isEmpty()) return

            val qst = files.find { it.name.endsWith(".qst", ignoreCase = true) }

            if (qst != null) {
                val parseResult = parseQstToQuest(readFile(qst).cursor(Endianness.Little))
                setResult(parseResult)

                if (parseResult is Success) {
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
                    setCurrentQuest(parseResult.value)
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
