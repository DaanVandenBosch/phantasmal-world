package world.phantasmal.web.viewer.controller

import mu.KotlinLogging
import org.w3c.files.File
import world.phantasmal.core.Failure
import world.phantasmal.core.PwResult
import world.phantasmal.core.Severity
import world.phantasmal.core.Success
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.cursor.cursor
import world.phantasmal.lib.fileFormats.ninja.parseNj
import world.phantasmal.lib.fileFormats.ninja.parseXj
import world.phantasmal.lib.fileFormats.ninja.parseXvm
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.web.viewer.store.ViewerStore
import world.phantasmal.webui.controllers.Controller
import world.phantasmal.webui.extension
import world.phantasmal.webui.readFile

private val logger = KotlinLogging.logger {}

class ViewerToolbarController(private val store: ViewerStore) : Controller() {
    private val _resultDialogVisible = mutableVal(false)
    private val _result = mutableVal<PwResult<*>?>(null)

    val resultDialogVisible: Val<Boolean> = _resultDialogVisible
    val result: Val<PwResult<*>?> = _result
    val resultMessage: Val<String> = result.map {
        when (it) {
            is Success, null -> "Encountered some problems while opening files."
            is Failure -> "An error occurred while opening files."
        }
    }

    suspend fun openFiles(files: List<File>) {
        val result = PwResult.build<Unit>(logger)
        var success = false

        try {
            var modelFound = false
            var textureFound = false

            for (file in files) {
                when (file.extension()?.toLowerCase()) {
                    "nj" -> {
                        if (modelFound) continue

                        modelFound = true
                        val njResult = parseNj(readFile(file).cursor(Endianness.Little))
                        result.addResult(njResult)

                        if (njResult is Success) {
                            store.setCurrentNinjaObject(njResult.value.firstOrNull())
                            success = true
                        }
                    }

                    "xj" -> {
                        if (modelFound) continue

                        modelFound = true
                        val xjResult = parseXj(readFile(file).cursor(Endianness.Little))
                        result.addResult(xjResult)

                        if (xjResult is Success) {
                            store.setCurrentNinjaObject(xjResult.value.firstOrNull())
                            success = true
                        }
                    }

                    "xvm" -> {
                        if (textureFound) continue

                        textureFound = true
                        val xvmResult = parseXvm(readFile(file).cursor(Endianness.Little))
                        result.addResult(xvmResult)

                        if (xvmResult is Success) {
                            store.setCurrentTextures(xvmResult.value.textures)
                            success = true
                        }
                    }

                    else -> {
                        result.addProblem(
                            Severity.Error,
                            """File "${file.name}" has an unsupported file type."""
                        )
                    }
                }
            }
        } catch (e: Exception) {
            result.addProblem(Severity.Error, "Couldn't parse files.", cause = e)
        }

        setResult(if (success) result.success(Unit) else result.failure())
    }

    fun dismissResultDialog() {
        _resultDialogVisible.value = false
    }

    private fun setResult(result: PwResult<*>?) {
        _result.value = result
        _resultDialogVisible.value = result != null && result.problems.isNotEmpty()
    }
}
