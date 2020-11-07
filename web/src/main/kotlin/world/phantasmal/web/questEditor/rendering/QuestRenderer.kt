package world.phantasmal.web.questEditor.rendering

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.core.rendering.Renderer
import world.phantasmal.web.externals.babylon.ArcRotateCamera
import world.phantasmal.web.externals.babylon.Engine
import world.phantasmal.web.externals.babylon.Vector3
import kotlin.math.PI

class QuestRenderer(canvas: HTMLCanvasElement, engine: Engine) : Renderer(canvas, engine) {
    override val camera = ArcRotateCamera("Camera", PI / 2, PI / 6, 500.0, Vector3.Zero(), scene)

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
            panningInertia = 0.0
            panningSensibility = 3.0
            panningAxis = Vector3(1.0, 0.0, -1.0)
            pinchDeltaPercentage = 0.1
            wheelDeltaPercentage = 0.1
        }
    }
}
