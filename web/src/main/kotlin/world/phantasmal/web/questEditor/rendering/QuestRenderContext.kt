package world.phantasmal.web.questEditor.rendering

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.core.rendering.RenderContext
import world.phantasmal.web.externals.three.Camera
import world.phantasmal.web.externals.three.Group
import world.phantasmal.web.externals.three.Object3D

class QuestRenderContext(
    canvas: HTMLCanvasElement,
    camera: Camera,
) : RenderContext(canvas, camera) {
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

    fun clearCollisionGeometry() {
        collisionGeometry = DEFAULT_COLLISION_GEOMETRY
    }

    companion object {
        private val DEFAULT_COLLISION_GEOMETRY = Group().apply {
            name = "Default Collision Geometry"
        }
    }
}
