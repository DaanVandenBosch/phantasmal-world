package world.phantasmal.web.questEditor.rendering

import world.phantasmal.web.core.rendering.DisposableThreeRenderer
import world.phantasmal.web.core.rendering.Renderer
import world.phantasmal.web.externals.three.Object3D
import world.phantasmal.web.externals.three.PerspectiveCamera

class QuestRenderer(
    createThreeRenderer: () -> DisposableThreeRenderer,
) : Renderer(
    createThreeRenderer,
    PerspectiveCamera(
        fov = 45.0,
        aspect = 1.0,
        near = 10.0,
        far = 5_000.0
    )
) {
    var collisionGeometry: Object3D? = null
        set(geom) {
            field?.let { scene.remove(it) }
            field = geom
            geom?.let { scene.add(it) }
        }

    init {
        camera.position.set(0.0, 50.0, 200.0)
        controls.update()

        controls.screenSpacePanning = false
    }

    fun resetCamera() {
    }

    fun enableCameraControls() {
    }

    fun disableCameraControls() {
    }

    override fun render() {
        super.render()
    }
}
