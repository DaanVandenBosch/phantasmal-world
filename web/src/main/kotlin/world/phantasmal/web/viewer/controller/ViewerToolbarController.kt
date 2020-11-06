package world.phantasmal.web.viewer.controller

import mu.KotlinLogging
import org.w3c.files.File
import world.phantasmal.core.PwResult
import world.phantasmal.core.Severity
import world.phantasmal.core.Success
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.cursor.cursor
import world.phantasmal.lib.fileFormats.ninja.parseNj
import world.phantasmal.lib.fileFormats.ninja.parseXj
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.web.viewer.store.ViewerStore
import world.phantasmal.webui.controllers.Controller
import world.phantasmal.webui.readFile

private val logger = KotlinLogging.logger {}

class ViewerToolbarController(private val store: ViewerStore) : Controller() {
    private val _resultDialogVisible = mutableVal(false)
    private val _result = mutableVal<PwResult<*>?>(null)

    val resultDialogVisible: Val<Boolean> = _resultDialogVisible
    val result: Val<PwResult<*>?> = _result

    suspend fun openFiles(files: List<File>) {
        var modelFileFound = false
        val result = PwResult.build<Nothing>(logger)

        try {
            for (file in files) {
                if (file.name.endsWith(".nj", ignoreCase = true)) {
                    if (modelFileFound) continue

                    modelFileFound = true
                    val njResult = parseNj(readFile(file).cursor(Endianness.Little))
                    result.addResult(njResult)

                    if (njResult is Success) {
                        store.setCurrentNinjaObject(njResult.value.firstOrNull())
                    }
                } else if (file.name.endsWith(".xj", ignoreCase = true)) {
                    if (modelFileFound) continue

                    modelFileFound = true
                    val xjResult = parseXj(readFile(file).cursor(Endianness.Little))
                    result.addResult(xjResult)

                    if (xjResult is Success) {
                        store.setCurrentNinjaObject(xjResult.value.firstOrNull())
                    }
                } else {
                    result.addProblem(
                        Severity.Error,
                        """File "${file.name}" has an unsupported file type."""
                    )
                }
            }
        } catch (e: Exception) {
            result.addProblem(Severity.Error, "Couldn't parse files.", cause = e)
        }

        // Set failure result, because setResult doesn't care about the type.
        setResult(result.failure())
    }

    private fun setResult(result: PwResult<*>) {
        _result.value = result

        if (result.problems.isNotEmpty()) {
            _resultDialogVisible.value = true
        }
    }
}
