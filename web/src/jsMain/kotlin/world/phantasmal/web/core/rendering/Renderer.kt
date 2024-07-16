package world.phantasmal.web.core.rendering

import kotlinx.browser.document
import kotlinx.browser.window
import mu.KotlinLogging
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.web.externals.three.Renderer as ThreeRenderer

private val logger = KotlinLogging.logger {}

abstract class Renderer : DisposableContainer() {
    protected abstract val context: RenderContext
    protected abstract val threeRenderer: ThreeRenderer
    protected abstract val inputManager: InputManager

    val canvas: HTMLCanvasElement get() = context.canvas

    private var rendering = false
    private var animationFrameHandle: Int = 0

    fun startRendering() {
        if (!rendering) {
            logger.trace { "${this::class.simpleName} - start rendering." }

            rendering = true
            renderLoop()
        }
    }

    fun stopRendering() {
        if (rendering) {
            logger.trace { "${this::class.simpleName} - stop rendering." }

            rendering = false
            window.cancelAnimationFrame(animationFrameHandle)
        }
    }

    open fun setSize(width: Int, height: Int) {
        if (width == 0 || height == 0) return

        context.width = width
        context.height = height
        context.canvas.width = width
        context.canvas.height = height

        threeRenderer.setSize(width.toDouble(), height.toDouble())

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
