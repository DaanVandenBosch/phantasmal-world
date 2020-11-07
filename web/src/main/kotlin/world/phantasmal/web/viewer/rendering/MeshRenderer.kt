package world.phantasmal.web.viewer.rendering

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.lib.fileFormats.ninja.NinjaObject
import world.phantasmal.web.core.rendering.Renderer
import world.phantasmal.web.core.rendering.conversion.ninjaObjectToVertexData
import world.phantasmal.web.externals.babylon.ArcRotateCamera
import world.phantasmal.web.externals.babylon.Engine
import world.phantasmal.web.externals.babylon.Mesh
import world.phantasmal.web.externals.babylon.Vector3
import world.phantasmal.web.viewer.store.ViewerStore
import kotlin.math.PI

class MeshRenderer(
    store: ViewerStore,
    canvas: HTMLCanvasElement,
    engine: Engine,
) : Renderer(canvas, engine) {
    private var mesh: Mesh? = null

    override val camera = ArcRotateCamera("Camera", PI / 2, PI / 3, 70.0, Vector3.Zero(), scene)

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
            panningSensibility = 10.0
            panningAxis = Vector3(1.0, 1.0, 0.0)
            pinchDeltaPercentage = 0.1
            wheelDeltaPercentage = 0.1
        }

        observe(store.currentNinjaObject, ::ninjaObjectOrXvmChanged)
    }

    override fun internalDispose() {
        mesh?.dispose()
        super.internalDispose()
    }

    private fun ninjaObjectOrXvmChanged(ninjaObject: NinjaObject<*>?) {
        mesh?.dispose()

        if (ninjaObject != null) {
            val mesh = Mesh("Model", scene)
            val vertexData = ninjaObjectToVertexData(ninjaObject)
            vertexData.applyToMesh(mesh)

            // Make sure we rotate around the center of the model instead of its origin.
            val bb = mesh.getBoundingInfo().boundingBox
            val height = bb.maximum.y - bb.minimum.y
            mesh.position = mesh.position.addInPlaceFromFloats(0.0, -height / 2 - bb.minimum.y, 0.0)

            this.mesh = mesh
        }
    }
}
