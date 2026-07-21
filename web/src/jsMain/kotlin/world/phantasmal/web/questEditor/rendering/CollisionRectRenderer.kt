package world.phantasmal.web.questEditor.rendering

import org.khronos.webgl.Float32Array
import world.phantasmal.web.externals.three.*
import world.phantasmal.webui.obj

/**
 * Renders collision rectangles as blue outlines on the ground for objects that have
 * collision width/depth properties, such as LaserFenceEx and LaserSquareFenceEx.
 * Uses thin mesh strips instead of LineSegments for visible thickness (WebGL caps linewidth at 1px).
 */
class CollisionRectRenderer {
    companion object {
        private const val COLLISION_COLOR = 0x4488FF
        private const val STRIP_HALF = 1.5f // half-thickness of the border strip
        private const val STRIP_HEIGHT = 1.0f // thin vertical extent so it's visible from angles
    }

    /**
     * Creates a flat rectangular outline on the ground representing the collision area.
     * The outline is built from 4 thin rectangular mesh strips (one per edge).
     */
    fun createCollisionRect(
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        width: Float,
        depth: Float,
        rotationY: Float,
    ): Object3D {
        val hw = width / 2f
        val hd = depth / 2f
        val t = STRIP_HALF
        val h = STRIP_HEIGHT

        // Build 4 edge strips as a single BufferGeometry.
        // Each strip is a box: 8 vertices, 12 triangles (top + bottom + 4 sides).
        // But since it's very thin we only need top+bottom faces (4 verts, 2 tris each strip).
        // Actually for visibility from all angles, use a simple flat box per edge.
        val vertices = mutableListOf<Float>()
        val indices = mutableListOf<Int>()

        // Each edge strip: a thin box lying on the ground.
        // We define 8 vertices (4 bottom y=0, 4 top y=h) and 12 triangles.
        fun addStrip(x1: Float, z1: Float, x2: Float, z2: Float) {
            // Edge direction
            val dx = x2 - x1
            val dz = z2 - z1
            val len = kotlin.math.sqrt((dx * dx + dz * dz).toDouble()).toFloat()
            if (len == 0f) return
            // Perpendicular normal (outward) for thickness
            val nx = -dz / len * t
            val nz = dx / len * t

            val base = vertices.size / 3

            // Bottom 4 vertices
            vertices.addAll(listOf(x1 - nx, 0f, z1 - nz))  // 0: inner start
            vertices.addAll(listOf(x1 + nx, 0f, z1 + nz))  // 1: outer start
            vertices.addAll(listOf(x2 + nx, 0f, z2 + nz))  // 2: outer end
            vertices.addAll(listOf(x2 - nx, 0f, z2 - nz))  // 3: inner end
            // Top 4 vertices
            vertices.addAll(listOf(x1 - nx, h, z1 - nz))   // 4
            vertices.addAll(listOf(x1 + nx, h, z1 + nz))   // 5
            vertices.addAll(listOf(x2 + nx, h, z2 + nz))   // 6
            vertices.addAll(listOf(x2 - nx, h, z2 - nz))   // 7

            // Bottom face
            indices.addAll(listOf(base, base + 2, base + 1))
            indices.addAll(listOf(base, base + 3, base + 2))
            // Top face
            indices.addAll(listOf(base + 4, base + 5, base + 6))
            indices.addAll(listOf(base + 4, base + 6, base + 7))
            // Front side
            indices.addAll(listOf(base + 1, base + 2, base + 6))
            indices.addAll(listOf(base + 1, base + 6, base + 5))
            // Back side
            indices.addAll(listOf(base, base + 4, base + 7))
            indices.addAll(listOf(base, base + 7, base + 3))
            // Left cap
            indices.addAll(listOf(base, base + 1, base + 5))
            indices.addAll(listOf(base, base + 5, base + 4))
            // Right cap
            indices.addAll(listOf(base + 3, base + 7, base + 6))
            indices.addAll(listOf(base + 3, base + 6, base + 2))
        }

        // 4 edges of the rectangle
        addStrip(-hw, -hd, hw, -hd) // bottom edge (near)
        addStrip(hw, -hd, hw, hd) // right edge
        addStrip(hw, hd, -hw, hd) // top edge (far)
        addStrip(-hw, hd, -hw, -hd) // left edge

        val geometry = BufferGeometry().apply {
            setAttribute("position", Float32BufferAttribute(Float32Array(vertices.toTypedArray()), 3))
            setIndex(indices.map { it.toDouble() }.toTypedArray())
        }

        val material = MeshBasicMaterial(obj {
            this.color = Color(COLLISION_COLOR)
            transparent = true
            this.opacity = 0.85
            side = DoubleSide
        })

        material.asDynamic().depthTest = false
        material.asDynamic().depthWrite = false

        return Mesh(geometry, material).apply {
            position.set(centerX.toDouble(), centerY.toDouble(), centerZ.toDouble())
            rotation.y = rotationY.toDouble()
            name = "CollisionRect"
            renderOrder = 1000
            frustumCulled = false
        }
    }

    /**
     * Updates the position and rotation of an existing collision rect.
     */
    fun updateCollisionRect(
        rect: Object3D,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        rotationY: Float,
    ) {
        rect.position.set(centerX.toDouble(), centerY.toDouble(), centerZ.toDouble())
        rect.rotation.y = rotationY.toDouble()
    }
}
