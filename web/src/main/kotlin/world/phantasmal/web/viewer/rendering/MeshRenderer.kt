package world.phantasmal.web.viewer.rendering

import world.phantasmal.web.core.rendering.DisposableThreeRenderer
import world.phantasmal.web.core.rendering.Renderer
import world.phantasmal.web.core.rendering.conversion.ninjaObjectToMesh
import world.phantasmal.web.core.rendering.disposeObject3DResources
import world.phantasmal.web.externals.three.BufferGeometry
import world.phantasmal.web.externals.three.Mesh
import world.phantasmal.web.externals.three.PerspectiveCamera
import world.phantasmal.web.viewer.store.ViewerStore

class MeshRenderer(
    private val store: ViewerStore,
    createThreeRenderer: () -> DisposableThreeRenderer,
) : Renderer(
    createThreeRenderer,
    PerspectiveCamera(
        fov = 45.0,
        aspect = 1.0,
        near = 1.0,
        far = 1_000.0,
    )
) {
    private var mesh: Mesh? = null

    init {
        initializeControls()
        camera.position.set(0.0, 25.0, 100.0)
        controls.target.set(0.0, 0.0, 0.0)
        controls.zoomSpeed = 2.0
        controls.screenSpacePanning = true
        controls.update()
        controls.saveState()

        observe(store.currentNinjaObject) {
            ninjaObjectOrXvmChanged(reset = true)
        }
        observe(store.currentTextures) {
            ninjaObjectOrXvmChanged(reset = false)
        }
    }

    private fun ninjaObjectOrXvmChanged(reset: Boolean) {
        mesh?.let { mesh ->
            disposeObject3DResources(mesh)
            scene.remove(mesh)
        }

        if (reset) {
            resetCamera()
        }

        val ninjaObject = store.currentNinjaObject.value
        val textures = store.currentTextures.value

        if (ninjaObject != null) {
            val mesh = ninjaObjectToMesh(ninjaObject, textures, boundingVolumes = true)

            // Make sure we rotate around the center of the model instead of its origin.
            val bb = (mesh.geometry as BufferGeometry).boundingBox!!
            val height = bb.max.y - bb.min.y
            mesh.translateY(-height / 2 - bb.min.y)
            scene.add(mesh)

            this.mesh = mesh
        }
    }
}
