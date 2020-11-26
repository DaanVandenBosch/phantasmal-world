package world.phantasmal.web.core.rendering

import kotlinx.browser.document
import kotlinx.browser.window
import mu.KotlinLogging
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.webui.DisposableContainer
import kotlin.math.floor
import world.phantasmal.web.externals.three.Renderer as ThreeRenderer

private val logger = KotlinLogging.logger {}

interface DisposableThreeRenderer : Disposable {
    val renderer: ThreeRenderer
}

abstract class Renderer : DisposableContainer() {
    protected abstract val context: RenderContext
    protected abstract val threeRenderer: ThreeRenderer
    protected abstract val inputManager: InputManager

    val canvas: HTMLCanvasElement get() = context.canvas

    private var rendering = false
    private var animationFrameHandle: Int = 0

    fun startRendering() {
        logger.trace { "${this::class.simpleName} - start rendering." }

        if (!rendering) {
            rendering = true
            renderLoop()
        }
    }

    fun stopRendering() {
        logger.trace { "${this::class.simpleName} - stop rendering." }

        rendering = false
        window.cancelAnimationFrame(animationFrameHandle)
    }

    open fun setSize(width: Double, height: Double) {
        if (width == 0.0 || height == 0.0) return

        context.width = width
        context.height = height
        context.canvas.width = floor(width).toInt()
        context.canvas.height = floor(height).toInt()

        threeRenderer.setSize(width, height)

        inputManager.setSize(width, height)
    }

    protected open fun render() {
        inputManager.beforeRender()

        threeRenderer.render(context.scene, context.camera)
    }

    private fun renderLoop() {
        if (rendering) {
            animationFrameHandle = window.requestAnimationFrame {
                try {
                    render()
                } finally {
                    renderLoop()
                }
            }
        }
    }

    companion object {
        fun createCanvas(): HTMLCanvasElement =
            (document.createElement("CANVAS") as HTMLCanvasElement).apply {
                tabIndex = 0
                style.outline = "none"
            }
    }
}
