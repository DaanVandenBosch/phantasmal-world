package world.phantasmal.web.questEditor.rendering

import world.phantasmal.web.externals.three.*
import world.phantasmal.webui.obj

/**
 * Renders range circles for objects that have radius properties, such as EventCollision.
 */
class RangeCircleRenderer {
    companion object {
        private const val SEGMENTS = 64
        private const val BRIGHT_RED_COLOR = 0xFF0000
    }

    fun createRangeCircle(
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        radius: Float,
        color: Int = BRIGHT_RED_COLOR,
        makeBolder: Boolean = false,
    ): Object3D {
        val ringThickness = if (makeBolder) 4.0f else 2.0f
        val geometry = createRingGeometry(
            innerRadius = radius - ringThickness,
            outerRadius = radius + ringThickness,
            segments = SEGMENTS,
        )

        val material = MeshBasicMaterial(obj {
            this.color = Color(color)
            transparent = true
            this.opacity = 0.8
            side = DoubleSide
        })
        material.asDynamic().depthTest = false
        material.asDynamic().depthWrite = false

        val elevatedY = centerY + 15.0f
        return Mesh(geometry, material).apply {
            position.set(centerX.toDouble(), elevatedY.toDouble(), centerZ.toDouble())
            name = if (makeBolder) "RangeRingBold" else "RangeRing"
            renderOrder = 1000
            frustumCulled = false
        }
    }

    /**
     * Updates the position of an existing range circle.
     * Note: For radius changes, it's better to recreate the circle.
     */
    fun updateRangeCircle(
        circle: Object3D,
        centerX: Float,
        centerY: Float,
        centerZ: Float
    ) {
        // Use same fixed height offset as when creating the circle
        val elevatedY = centerY + 15.0f
        circle.position.set(centerX.toDouble(), elevatedY.toDouble(), centerZ.toDouble())
        // Note: Radius updates are complex with the current ThreeJS bindings.
        // For now, we only update position. Radius changes should recreate the circle.
    }
}