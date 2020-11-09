package world.phantasmal.web.questEditor.rendering

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.core.rendering.Renderer
import world.phantasmal.web.externals.babylon.ArcRotateCamera
import world.phantasmal.web.externals.babylon.Engine
import world.phantasmal.web.externals.babylon.TransformNode
import world.phantasmal.web.externals.babylon.Vector3
import kotlin.math.PI
import kotlin.math.max

class QuestRenderer(canvas: HTMLCanvasElement, engine: Engine) : Renderer(canvas, engine) {
    override val camera = ArcRotateCamera("Camera", PI / 2, PI / 6, 500.0, Vector3.Zero(), scene)

    var collisionGeometry: TransformNode? = null

    init {
        with(camera) {
            attachControl(
                canvas,
                noPreventDefault = false,
                useCtrlForPanning = false,
                panningMouseButton = 0
            )
            inertia = 0.0
            angularSensibilityX = 200.0
            angularSensibilityY = 200.0
            // Set lowerBetaLimit to avoid shitty camera implementation from breaking completely
            // when looking directly down.
            lowerBetaLimit = 0.4
            panningInertia = 0.0
            panningAxis = Vector3(1.0, 0.0, -1.0)
            pinchDeltaPercentage = 0.1
            wheelDeltaPercentage = 0.1

            updatePanningSensibility()
            onViewMatrixChangedObservable.add({ _, _ ->
                updatePanningSensibility()
            })

            camera.storeState()
        }
    }

    fun resetCamera() {
        camera.restoreState()
    }

    override fun render() {
        camera.minZ = max(0.01, camera.radius / 100)
        camera.maxZ = max(2_000.0, 10 * camera.radius)
        super.render()
    }

    /**
     * Make "panningSensibility" an inverse function of radius to make panning work "sensibly"
     * at all distances.
     */
    private fun updatePanningSensibility() {
        camera.panningSensibility = 1_000 / camera.radius
    }
}
