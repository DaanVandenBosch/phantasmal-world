package world.phantasmal.web.core.rendering

import kotlinx.browser.window
import mu.KotlinLogging
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.web.core.minus
import world.phantasmal.web.externals.three.*
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.obj
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import world.phantasmal.web.externals.three.Renderer as ThreeRenderer

private val logger = KotlinLogging.logger {}

interface DisposableThreeRenderer : Disposable {
    val renderer: ThreeRenderer
}

abstract class Renderer(
    createThreeRenderer: () -> DisposableThreeRenderer,
    val camera: Camera,
) : DisposableContainer() {
    private val threeRenderer: ThreeRenderer = addDisposable(createThreeRenderer()).renderer
    private val light = HemisphereLight(
        skyColor = 0xffffff,
        groundColor = 0x505050,
        intensity = 1.0
    )
    private val lightHolder = Group().add(light)

    private var rendering = false
    private var animationFrameHandle: Int = 0

    val canvas: HTMLCanvasElement =
        threeRenderer.domElement.apply {
            tabIndex = 0
            style.outline = "none"
        }

    val scene: Scene =
        Scene().apply {
            background = Color(0x181818)
            add(lightHolder)
        }

    val controls: OrbitControls =
        OrbitControls(camera, canvas).apply {
            mouseButtons = obj {
                LEFT = MOUSE.PAN
                MIDDLE = MOUSE.DOLLY
                RIGHT = MOUSE.ROTATE
            }
        }

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
        canvas.width = floor(width).toInt()
        canvas.height = floor(height).toInt()
        threeRenderer.setSize(width, height)

        if (camera is PerspectiveCamera) {
            camera.aspect = width / height
            camera.updateProjectionMatrix()
        } else if (camera is OrthographicCamera) {
            camera.left = -floor(width / 2)
            camera.right = ceil(width / 2)
            camera.top = floor(height / 2)
            camera.bottom = -ceil(height / 2)
            camera.updateProjectionMatrix()
        }

        controls.update()
    }

    protected open fun render() {
        if (camera is PerspectiveCamera) {
            val distance = (controls.target - camera.position).length()
            camera.near = distance / 100
            camera.far = max(2_000.0, 10 * distance)
            camera.updateProjectionMatrix()
        }

        threeRenderer.render(scene, camera)
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
}
