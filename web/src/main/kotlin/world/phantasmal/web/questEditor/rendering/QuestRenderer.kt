package world.phantasmal.web.questEditor.rendering

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.core.newJsObject
import world.phantasmal.web.core.rendering.Renderer
import world.phantasmal.web.externals.*
import kotlin.math.PI

class QuestRenderer(
    canvas: HTMLCanvasElement,
    createEngine: (HTMLCanvasElement) -> Engine,
) : Renderer(canvas, createEngine) {
    private val camera = ArcRotateCamera("Camera", PI / 2, PI / 2, 2.0, Vector3.Zero(), scene)
    private val light = HemisphericLight("Light", Vector3(1.0, 1.0, 0.0), scene)
    private val cylinder =
        MeshBuilder.CreateCylinder("Cylinder", newJsObject { diameter = 1.0 }, scene)

    init {
        camera.attachControl(canvas, noPreventDefault = true)
    }

    override fun internalDispose() {
        camera.dispose()
        light.dispose()
        cylinder.dispose()
        super.internalDispose()
    }
}
