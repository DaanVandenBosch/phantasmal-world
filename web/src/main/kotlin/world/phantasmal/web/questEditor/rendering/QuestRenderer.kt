package world.phantasmal.web.questEditor.rendering

import world.phantasmal.web.core.rendering.DisposableThreeRenderer
import world.phantasmal.web.core.rendering.Renderer
import world.phantasmal.web.externals.three.Group
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
    val entities: Object3D = Group().apply {
        name = "Entities"
        scene.add(this)
    }

    var collisionGeometry: Object3D = DEFAULT_COLLISION_GEOMETRY
        set(geom) {
            scene.remove(field)
            field = geom
            scene.add(geom)
        }

    init {
        camera.position.set(0.0, 50.0, 200.0)
    }

    override fun initializeControls() {
        super.initializeControls()
        controls.screenSpacePanning = false
        controls.update()
    }

    fun resetCamera() {
        // TODO: Camera reset.
    }

    fun clearCollisionGeometry() {
        collisionGeometry = DEFAULT_COLLISION_GEOMETRY
    }

    companion object {
        private val DEFAULT_COLLISION_GEOMETRY = Group().apply {
            name = "Default Collision Geometry"
        }
    }
}
