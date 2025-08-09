package world.phantasmal.web.questEditor.rendering

import org.khronos.webgl.Float32Array
import world.phantasmal.web.externals.three.*
import world.phantasmal.webui.obj
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * Renders range circles for objects that have radius properties, such as EventCollision.
 */
class RangeCircleRenderer {
    companion object {
        private const val SEGMENTS = 64  // More segments for smoother circle
        private const val BRIGHT_RED_COLOR = 0xFF0000  // Bright red color, very visible
        private const val LINE_WIDTH = 12.0  // Very thick line for maximum visibility
        private const val OPACITY = 1.0
    }

    /**
     * Creates a yellow range circle using line segments for visualizing collision/event ranges.
     */
    fun createRangeCircle(
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        radius: Float,
        color: Int = BRIGHT_RED_COLOR
    ): Object3D {
        // Create circle vertices
        val vertices = mutableListOf<Float>()
        for (i in 0..SEGMENTS) {
            val angle = (i * 2 * PI / SEGMENTS)
            val x = (cos(angle) * radius).toFloat()
            val z = (sin(angle) * radius).toFloat()
            vertices.add(x)
            vertices.add(0.0f) // Y is always 0 (ground level)
            vertices.add(z)
        }
        
        val geometry = BufferGeometry().apply {
            setAttribute("position", Float32BufferAttribute(Float32Array(vertices.toTypedArray()), 3))
        }
        
        val material = LineBasicMaterial(obj {
            this.color = Color(color)
            linewidth = LINE_WIDTH
            transparent = false
            opacity = OPACITY
        })
        
        // Simple single-layer approach for maximum clarity
        return Line(geometry, material).apply {
            position.set(centerX.toDouble(), centerY.toDouble(), centerZ.toDouble())
            name = "RangeCircle"
            renderOrder = 1000 // Render after other objects
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
        centerZ: Float,
        radius: Float
    ) {
        circle.position.set(centerX.toDouble(), centerY.toDouble(), centerZ.toDouble())
        // Note: Radius updates are complex with the current ThreeJS bindings.
        // For now, we only update position. Radius changes should recreate the circle.
        // The circle is now a Group containing multiple Line objects, so position update works for all
    }
}