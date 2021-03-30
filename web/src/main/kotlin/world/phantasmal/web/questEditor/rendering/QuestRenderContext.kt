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

    var collisionGeometryVisible = true
        set(visible) {
            field = visible
            collisionGeometry.visible = visible
        }

    var renderGeometryVisible = false
        set(visible) {
            field = visible
            renderGeometry.visible = visible
        }

    var collisionGeometry: Object3D = DEFAULT_COLLISION_GEOMETRY
        set(geom) {
            scene.remove(field)
            geom.visible = collisionGeometryVisible
            field = geom
            scene.add(geom)
        }

    var renderGeometry: Object3D = DEFAULT_RENDER_GEOMETRY
        set(geom) {
            scene.remove(field)
            geom.visible = renderGeometryVisible
            field = geom
            scene.add(geom)
        }

    fun clearCollisionGeometry() {
        collisionGeometry = DEFAULT_COLLISION_GEOMETRY
    }

    fun clearRenderGeometry() {
        renderGeometry = DEFAULT_RENDER_GEOMETRY
    }

    companion object {
        private val DEFAULT_COLLISION_GEOMETRY = Group().apply {
            name = "Default Collision Geometry"
        }
        private val DEFAULT_RENDER_GEOMETRY = Group().apply {
            name = "Default Render Geometry"
        }
    }
}
