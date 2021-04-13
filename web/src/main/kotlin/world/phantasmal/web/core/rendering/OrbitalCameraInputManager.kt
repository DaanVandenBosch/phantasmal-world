package world.phantasmal.web.core.rendering

import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.pointerevents.PointerEvent
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.web.externals.three.*
import world.phantasmal.webui.dom.disposableListener
import world.phantasmal.webui.obj
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

class OrbitalCameraInputManager(
    canvas: HTMLCanvasElement,
    private val camera: Camera,
    position: Vector3,
    screenSpacePanning: Boolean,
    enableRotate: Boolean = true,
) : TrackedDisposable(), InputManager {
    private lateinit var controls: OrbitControls

    @Suppress("JoinDeclarationAndAssignment")
    private val pointerDownListener: Disposable

    var enabled: Boolean
        get() = controls.enabled
        set(enabled) {
            controls.enabled = enabled
        }

    init {
        // Switch mouse button actions when certain modifier keys are pressed to counteract
        // OrbitControls switching left and right click behavior in that case.
        pointerDownListener = canvas.disposableListener<PointerEvent>("pointerdown", {
            if (it.ctrlKey || it.metaKey || it.shiftKey) {
                controls.mouseButtons = obj {
                    LEFT = MOUSE.ROTATE
                    MIDDLE = MOUSE.DOLLY
                    RIGHT = MOUSE.PAN
                }
            } else {
                controls.mouseButtons = obj {
                    LEFT = MOUSE.PAN
                    MIDDLE = MOUSE.DOLLY
                    RIGHT = MOUSE.ROTATE
                }
            }
        })

        // Ensure OrbitControls is instantiated after the pointerdown event listener is attached.
        controls = OrbitControls(camera, canvas)

        controls.mouseButtons = obj {
            LEFT = MOUSE.PAN
            MIDDLE = MOUSE.DOLLY
            RIGHT = MOUSE.ROTATE
        }

        controls.touches = obj {
            ONE = TOUCH.PAN
            TWO = TOUCH.DOLLY_ROTATE
        }

        camera.position.copy(position)
        controls.screenSpacePanning = screenSpacePanning
        controls.enableRotate = enableRotate
        controls.zoomSpeed = 3.0
        controls.update()
        controls.saveState()
    }

    override fun dispose() {
        controls.dispose()
        pointerDownListener.dispose()
        super.dispose()
    }

    fun setTarget(target: Vector3) {
        controls.target.copy(target)
        controls.update()
    }

    fun lookAt(position: Vector3, target: Vector3) {
        camera.position.copy(position)
        controls.target.copy(target)
        controls.update()
    }

    override fun resetCamera() {
        controls.reset()
    }

    override fun setSize(width: Int, height: Int) {
        if (width == 0 || height == 0) return

        if (camera is PerspectiveCamera) {
            camera.aspect = width.toDouble() / height
            camera.updateProjectionMatrix()
        } else if (camera is OrthographicCamera) {
            camera.left = -floor(width / 2.0)
            camera.right = ceil(width / 2.0)
            camera.top = floor(height / 2.0)
            camera.bottom = -ceil(height / 2.0)
            camera.updateProjectionMatrix()
        }

        controls.update()
    }

    override fun beforeRender() {
        if (camera is PerspectiveCamera) {
            val distance = camera.position.distanceTo(controls.target)
            camera.near = max(.01, distance / 100)
            camera.far = max(2_000.0, 10 * distance)
            camera.updateProjectionMatrix()
        }
    }
}
