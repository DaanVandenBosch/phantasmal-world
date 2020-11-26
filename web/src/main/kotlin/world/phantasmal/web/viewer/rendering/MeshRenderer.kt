package world.phantasmal.web.viewer.rendering

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.core.rendering.*
import world.phantasmal.web.core.rendering.conversion.ninjaObjectToMesh
import world.phantasmal.web.externals.three.BufferGeometry
import world.phantasmal.web.externals.three.Mesh
import world.phantasmal.web.externals.three.PerspectiveCamera
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.web.viewer.store.ViewerStore

class MeshRenderer(
    private val viewerStore: ViewerStore,
    createThreeRenderer: (HTMLCanvasElement) -> DisposableThreeRenderer,
) : Renderer() {
    override val context = addDisposable(RenderContext(
        createCanvas(),
        PerspectiveCamera(
            fov = 45.0,
            aspect = 1.0,
            near = 10.0,
            far = 5_000.0
        )
    ))

    override val threeRenderer = addDisposable(createThreeRenderer(context.canvas)).renderer

    override val inputManager = addDisposable(OrbitalCameraInputManager(
        context.canvas,
        context.camera,
        Vector3(0.0, 25.0, 100.0),
        screenSpacePanning = true
    ))

    private var mesh: Mesh? = null

    init {
        observe(viewerStore.currentNinjaObject) {
            ninjaObjectOrXvmChanged(reset = true)
        }
        observe(viewerStore.currentTextures) {
            ninjaObjectOrXvmChanged(reset = false)
        }
    }

    private fun ninjaObjectOrXvmChanged(reset: Boolean) {
        mesh?.let { mesh ->
            disposeObject3DResources(mesh)
            context.scene.remove(mesh)
        }

        if (reset) {
            inputManager.resetCamera()
        }

        val ninjaObject = viewerStore.currentNinjaObject.value
        val textures = viewerStore.currentTextures.value

        if (ninjaObject != null) {
            val mesh = ninjaObjectToMesh(ninjaObject, textures, boundingVolumes = true)

            // Make sure we rotate around the center of the model instead of its origin.
            val bb = (mesh.geometry as BufferGeometry).boundingBox!!
            val height = bb.max.y - bb.min.y
            mesh.translateY(-height / 2 - bb.min.y)
            context.scene.add(mesh)

            this.mesh = mesh
        }
    }
}
