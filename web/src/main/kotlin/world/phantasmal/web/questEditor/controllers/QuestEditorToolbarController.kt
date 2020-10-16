package world.phantasmal.web.questEditor.controllers

import kotlinx.coroutines.launch
import org.w3c.files.File
import world.phantasmal.core.disposable.Scope
import world.phantasmal.webui.controllers.Controller
import world.phantasmal.webui.readFile

class QuestEditorToolbarController(
    scope: Scope,
) : Controller(scope) {
    fun filesOpened(files: List<File>) {
        launch {
            if (files.isEmpty()) return@launch

            val qst = files.find { it.name.endsWith(".qst", ignoreCase = true) }

            if (qst != null) {
                val buffer = readFile(qst)
                // TODO: Parse qst.
            } else {
                val bin = files.find { it.name.endsWith(".bin", ignoreCase = true) }
                val dat = files.find { it.name.endsWith(".dat", ignoreCase = true) }

                if (bin != null && dat != null) {
                    val binBuffer = readFile(bin)
                    val datBuffer = readFile(dat)
                    // TODO: Parse bin and dat.
                }
            }
        }
    }
}
