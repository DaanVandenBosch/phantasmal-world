package world.phantasmal.web.core.rendering

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.web.externals.three.*
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
    private val controls = OrbitControls(camera, canvas)

    var enabled: Boolean
        get() = controls.enabled
        set(enabled) {
            controls.enabled = enabled
        }

    init {
        controls.mouseButtons = obj {
            LEFT = MOUSE.PAN
            MIDDLE = MOUSE.DOLLY
            RIGHT = MOUSE.ROTATE
        }

        camera.position.copy(position)
        controls.screenSpacePanning = screenSpacePanning
        controls.enableRotate = enableRotate
        controls.update()
        controls.saveState()
    }

    override fun internalDispose() {
        controls.dispose()
        super.internalDispose()
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

    override fun setSize(width: Double, height: Double) {
        if (width == 0.0 || height == 0.0) return

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

    override fun beforeRender() {
        if (camera is PerspectiveCamera) {
            val distance = camera.position.distanceTo(controls.target)
            camera.near = max(.01, distance / 100)
            camera.far = max(2_000.0, 10 * distance)
            camera.updateProjectionMatrix()
        }
    }
}
