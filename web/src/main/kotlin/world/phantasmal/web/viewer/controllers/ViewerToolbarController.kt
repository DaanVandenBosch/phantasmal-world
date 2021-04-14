package world.phantasmal.web.viewer.controllers

import mu.KotlinLogging
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
import world.phantasmal.lib.fileFormats.parseAreaCollisionGeometry
import world.phantasmal.lib.fileFormats.parseAreaRenderGeometry
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.web.core.files.cursor
import world.phantasmal.web.viewer.stores.NinjaGeometry
import world.phantasmal.web.viewer.stores.ViewerStore
import world.phantasmal.webui.controllers.Controller
import world.phantasmal.webui.files.FileHandle

private val logger = KotlinLogging.logger {}

class ViewerToolbarController(private val store: ViewerStore) : Controller() {
    private val _resultDialogVisible = mutableVal(false)
    private val _result = mutableVal<PwResult<*>?>(null)

    val applyTexturesEnabled: Val<Boolean> = store.applyTexturesEnabled
    val applyTextures: Val<Boolean> = store.applyTextures
    val showSkeletonEnabled: Val<Boolean> = store.showSkeletonEnabled
    val showSkeleton: Val<Boolean> = store.showSkeleton
    val playAnimation: Val<Boolean> = store.animationPlaying
    val frameRate: Val<Int> = store.frameRate
    val frame: Val<Int> = store.frame
    val animationControlsEnabled: Val<Boolean> = store.currentNinjaMotion.isNotNull()
    val maxFrame: Val<String> = store.currentNinjaMotion.map { "/ ${it?.frameCount ?: 0}" }
    val resultDialogVisible: Val<Boolean> = _resultDialogVisible
    val result: Val<PwResult<*>?> = _result
    val resultMessage: Val<String> = result.map {
        when (it) {
            is Success, null -> "Encountered some problems while opening files."
            is Failure -> "An error occurred while opening files."
        }
    }

    fun setApplyTextures(apply: Boolean) {
        store.setApplyTextures(apply)
    }

    fun setShowSkeleton(show: Boolean) {
        store.setShowSkeleton(show)
    }

    fun setPlayAnimation(play: Boolean) {
        store.setAnimationPlaying(play)
    }

    fun setFrameRate(frameRate: Int) {
        store.setFrameRate(frameRate)
    }

    fun setFrame(frame: Int) {
        store.setFrame(frame)
    }

    suspend fun clearCurrentAnimation() {
        store.setCurrentAnimation(null)
    }

    suspend fun openFiles(files: List<FileHandle>?) {
        files ?: return

        val result = PwResult.build<Unit>(logger)
        var success = false

        try {
            var ninjaGeometry: NinjaGeometry? = null
            var textures: List<XvrTexture>? = null
            var ninjaMotion: NjMotion? = null

            for (file in files) {
                val extension = file.extension()?.toLowerCase()

                val cursor = file.cursor(Endianness.Little)
                var fileResult: PwResult<*>

                when (extension) {
                    "nj" -> {
                        val njResult = parseNj(cursor)
                        fileResult = njResult

                        if (njResult is Success) {
                            ninjaGeometry = njResult.value.firstOrNull()?.let(NinjaGeometry::Object)
                        }
                    }

                    "xj" -> {
                        val xjResult = parseXj(cursor)
                        fileResult = xjResult

                        if (xjResult is Success) {
                            ninjaGeometry = xjResult.value.firstOrNull()?.let(NinjaGeometry::Object)
                        }
                    }

                    "rel" -> {
                        // TODO: Detect .rel type instead of relying on filename.
                        if (file.name.endsWith("c.rel")) {
                            val collisionGeometry = parseAreaCollisionGeometry(cursor)
                            fileResult = Success(collisionGeometry)
                            ninjaGeometry = NinjaGeometry.Collision(collisionGeometry)
                        } else {
                            val renderGeometry = parseAreaRenderGeometry(cursor)
                            fileResult = Success(renderGeometry)
                            ninjaGeometry = NinjaGeometry.Render(renderGeometry)
                        }
                    }

                    "afs" -> {
                        val afsResult = parseAfsTextures(cursor)
                        fileResult = afsResult

                        if (afsResult is Success) {
                            textures = afsResult.value
                        }
                    }

                    "xvm" -> {
                        val xvmResult = parseXvm(cursor)
                        fileResult = xvmResult

                        if (xvmResult is Success) {
                            textures = xvmResult.value.textures
                        }
                    }

                    "njm" -> {
                        val njm = parseNjm(cursor)
                        fileResult = Success(njm)
                        ninjaMotion = njm
                    }

                    else -> {
                        result.addProblem(
                            Severity.Error,
                            """File "${file.name}" has an unsupported file type.""",
                        )
                        continue
                    }
                }

                result.addResult(fileResult)

                if (fileResult is Success<*>) {
                    success = true
                }
            }

            ninjaGeometry?.let(store::setCurrentNinjaGeometry)
            textures?.let(store::setCurrentTextures)
            ninjaMotion?.let(store::setCurrentNinjaMotion)
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
}
