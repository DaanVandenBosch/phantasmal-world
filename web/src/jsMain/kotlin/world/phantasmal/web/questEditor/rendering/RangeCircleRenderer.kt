package world.phantasmal.web.questEditor.rendering

import org.khronos.webgl.Float32Array
import world.phantasmal.web.externals.three.*
import world.phantasmal.webui.obj
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
     * Creates a range circle using thick ring geometry like sectionId labels.
     * Can optionally make it bolder for script collisions.
     * Uses fixed height offset like sectionId labels for consistent display on slopes.
     */
    fun createRangeCircle(
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        radius: Float,
        color: Int = BRIGHT_RED_COLOR,
        makeBolder: Boolean = false
    ): Object3D {
        // Create thick ring geometry similar to sectionId labels
        val ringThickness = if (makeBolder) 4.0f else 2.0f  // Thicker ring for ScriptCollision
        val innerRadius = radius - ringThickness
        val outerRadius = radius + ringThickness
        val height = 1.0f  // Very thin height for ring appearance

        // Create ring vertices manually
        val vertices = mutableListOf<Float>()
        val indices = mutableListOf<Int>()

        // Create ring by making two circles and connecting them
        for (i in 0 until SEGMENTS) {
            val angle1 = (i * 2 * PI / SEGMENTS)
            val angle2 = ((i + 1) * 2 * PI / SEGMENTS)

            // Outer circle bottom
            val x1o = (cos(angle1) * outerRadius).toFloat()
            val z1o = (sin(angle1) * outerRadius).toFloat()
            val x2o = (cos(angle2) * outerRadius).toFloat()
            val z2o = (sin(angle2) * outerRadius).toFloat()

            // Inner circle bottom
            val x1i = (cos(angle1) * innerRadius).toFloat()
            val z1i = (sin(angle1) * innerRadius).toFloat()
            val x2i = (cos(angle2) * innerRadius).toFloat()
            val z2i = (sin(angle2) * innerRadius).toFloat()

            val baseIndex = vertices.size / 3

            // Add vertices for this segment (8 vertices: 4 bottom, 4 top)
            // Bottom vertices
            vertices.addAll(listOf(x1o, 0f, z1o))  // 0: outer1 bottom
            vertices.addAll(listOf(x2o, 0f, z2o))  // 1: outer2 bottom
            vertices.addAll(listOf(x1i, 0f, z1i))  // 2: inner1 bottom
            vertices.addAll(listOf(x2i, 0f, z2i))  // 3: inner2 bottom

            // Top vertices
            vertices.addAll(listOf(x1o, height, z1o))  // 4: outer1 top
            vertices.addAll(listOf(x2o, height, z2o))  // 5: outer2 top
            vertices.addAll(listOf(x1i, height, z1i))  // 6: inner1 top
            vertices.addAll(listOf(x2i, height, z2i))  // 7: inner2 top

            // Create faces for this ring segment
            // Bottom face (ring shape)
            indices.addAll(listOf(baseIndex, baseIndex + 2, baseIndex + 1))
            indices.addAll(listOf(baseIndex + 1, baseIndex + 2, baseIndex + 3))

            // Top face (ring shape)
            indices.addAll(listOf(baseIndex + 4, baseIndex + 5, baseIndex + 6))
            indices.addAll(listOf(baseIndex + 5, baseIndex + 7, baseIndex + 6))

            // Outer wall
            indices.addAll(listOf(baseIndex, baseIndex + 1, baseIndex + 4))
            indices.addAll(listOf(baseIndex + 1, baseIndex + 5, baseIndex + 4))

            // Inner wall
            indices.addAll(listOf(baseIndex + 2, baseIndex + 6, baseIndex + 3))
            indices.addAll(listOf(baseIndex + 3, baseIndex + 6, baseIndex + 7))
        }

        val geometry = BufferGeometry().apply {
            setAttribute("position", Float32BufferAttribute(Float32Array(vertices.toTypedArray()), 3))
            setIndex(indices.map { it.toDouble() }.toTypedArray())
        }

        val material = MeshBasicMaterial(obj {
            this.color = Color(color)
            transparent = true
            this.opacity = 0.8
            side = DoubleSide
        })

        // Force material settings
        material.asDynamic().depthTest = false
        material.asDynamic().depthWrite = false

        // Use fixed height offset like sectionId labels for consistent display on slopes
        val elevatedY = centerY + 15.0f // Fixed offset above object position
        return Mesh(geometry, material).apply {
            position.set(centerX.toDouble(), elevatedY.toDouble(), centerZ.toDouble())
            name = if (makeBolder) "RangeRingBold" else "RangeRing"
            renderOrder = 1000 // Render after other objects
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