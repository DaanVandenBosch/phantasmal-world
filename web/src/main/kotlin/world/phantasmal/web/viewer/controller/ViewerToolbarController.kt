package world.phantasmal.web.viewer.controller

import mu.KotlinLogging
import org.w3c.files.File
import world.phantasmal.core.Failure
import world.phantasmal.core.PwResult
import world.phantasmal.core.Severity
import world.phantasmal.core.Success
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.compression.prs.prsDecompress
import world.phantasmal.lib.cursor.Cursor
import world.phantasmal.lib.cursor.cursor
import world.phantasmal.lib.fileFormats.ninja.*
import world.phantasmal.lib.fileFormats.parseAfs
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
            val kindsFound = mutableSetOf<FileKind>()

            for (file in files) {
                val extension = file.extension()?.toLowerCase()

                val kind = when (extension) {
                    "nj", "xj" -> FileKind.Model
                    "afs", "xvm" -> FileKind.Texture
                    else -> {
                        result.addProblem(
                            Severity.Error,
                            """File "${file.name}" has an unsupported file type.""",
                        )
                        continue
                    }
                }

                if (kind in kindsFound) continue

                val cursor = readFile(file).cursor(Endianness.Little)
                var fileResult: PwResult<*>? = null

                when (extension) {
                    "nj" -> {
                        val njResult = parseNj(cursor)
                        fileResult = njResult

                        if (njResult is Success) {
                            store.setCurrentNinjaObject(njResult.value.firstOrNull())
                        }
                    }

                    "xj" -> {
                        val xjResult = parseXj(cursor)
                        fileResult = xjResult

                        if (xjResult is Success) {
                            store.setCurrentNinjaObject(xjResult.value.firstOrNull())
                        }
                    }

                    "afs" -> {
                        val afsResult = parseAfsTextures(cursor)
                        fileResult = afsResult

                        if (afsResult is Success) {
                            store.setCurrentTextures(afsResult.value)
                        }
                    }

                    "xvm" -> {
                        val xvmResult = parseXvm(cursor)
                        fileResult = xvmResult

                        if (xvmResult is Success) {
                            store.setCurrentTextures(xvmResult.value.textures)
                        }
                    }
                }

                fileResult?.let(result::addResult)

                if (fileResult is Success<*>) {
                    success = true
                    kindsFound.add(kind)
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

    private fun parseAfsTextures(cursor: Cursor): PwResult<List<XvrTexture>> {
        val result = PwResult.build<List<XvrTexture>>(logger)
        val afsResult = parseAfs(cursor)
        result.addResult(afsResult)

        if (afsResult !is Success) {
            return result.failure()
        }

        if (afsResult.value.isEmpty()) {
            result.addProblem(Severity.Info, "AFS archive is empty.")
        }

        val textures: List<XvrTexture> = afsResult.value.flatMap { file ->
            val fileCursor = file.cursor()

            val decompressedCursor: Cursor =
                if (isXvm(fileCursor)) {
                    fileCursor
                } else {
                    val decompressionResult = prsDecompress(fileCursor)
                    result.addResult(decompressionResult)

                    if (decompressionResult !is Success) {
                        return@flatMap emptyList()
                    }

                    decompressionResult.value
                }

            val xvmResult = parseXvm(decompressedCursor)
            result.addResult(xvmResult)

            if (xvmResult is Success) xvmResult.value.textures else emptyList()
        }

        return result.success(textures)
    }

    private enum class FileKind {
        Model, Texture
    }
}
