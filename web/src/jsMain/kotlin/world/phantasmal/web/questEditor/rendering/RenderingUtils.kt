package world.phantasmal.web.questEditor.rendering

import org.khronos.webgl.Float32Array
import world.phantasmal.web.externals.three.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Creates a ring geometry (annulus) with the given inner/outer radii and segment count.
 * The ring is flat on the XZ plane with a thin height for visibility.
 */
fun createRingGeometry(
    innerRadius: Float,
    outerRadius: Float,
    segments: Int,
    height: Float = 1.0f,
): BufferGeometry {
    val vertices = mutableListOf<Float>()
    val indices = mutableListOf<Int>()

    for (i in 0 until segments) {
        val angle1 = (i * 2 * PI / segments)
        val angle2 = ((i + 1) * 2 * PI / segments)

        val x1o = (cos(angle1) * outerRadius).toFloat()
        val z1o = (sin(angle1) * outerRadius).toFloat()
        val x2o = (cos(angle2) * outerRadius).toFloat()
        val z2o = (sin(angle2) * outerRadius).toFloat()

        val x1i = (cos(angle1) * innerRadius).toFloat()
        val z1i = (sin(angle1) * innerRadius).toFloat()
        val x2i = (cos(angle2) * innerRadius).toFloat()
        val z2i = (sin(angle2) * innerRadius).toFloat()

        val b = vertices.size / 3

        // Bottom vertices
        vertices.addAll(listOf(x1o, 0f, z1o))   // 0: outer1 bottom
        vertices.addAll(listOf(x2o, 0f, z2o))   // 1: outer2 bottom
        vertices.addAll(listOf(x1i, 0f, z1i))   // 2: inner1 bottom
        vertices.addAll(listOf(x2i, 0f, z2i))   // 3: inner2 bottom
        // Top vertices
        vertices.addAll(listOf(x1o, height, z1o)) // 4: outer1 top
        vertices.addAll(listOf(x2o, height, z2o)) // 5: outer2 top
        vertices.addAll(listOf(x1i, height, z1i)) // 6: inner1 top
        vertices.addAll(listOf(x2i, height, z2i)) // 7: inner2 top

        // Bottom face
        indices.addAll(listOf(b, b + 2, b + 1))
        indices.addAll(listOf(b + 1, b + 2, b + 3))
        // Top face
        indices.addAll(listOf(b + 4, b + 5, b + 6))
        indices.addAll(listOf(b + 5, b + 7, b + 6))
        // Outer wall
        indices.addAll(listOf(b, b + 1, b + 4))
        indices.addAll(listOf(b + 1, b + 5, b + 4))
        // Inner wall
        indices.addAll(listOf(b + 2, b + 6, b + 3))
        indices.addAll(listOf(b + 3, b + 6, b + 7))
    }

    return BufferGeometry().apply {
        setAttribute("position", Float32BufferAttribute(Float32Array(vertices.toTypedArray()), 3))
        setIndex(indices.map { it.toDouble() }.toTypedArray())
    }
}

/**
 * Updates a mesh to face the camera (billboard effect) and maintain constant screen size.
 */
fun updateBillboardScale(camera: Camera, mesh: Mesh, baseDistance: Double = 1000.0) {
    val textWorldPosition = Vector3()
    mesh.asDynamic().getWorldPosition(textWorldPosition)
    val distance = camera.position.distanceTo(textWorldPosition)
    val scaleFactor = (distance / baseDistance).coerceIn(0.1, 10.0)
    mesh.scale.asDynamic().setScalar(scaleFactor)
    mesh.asDynamic().lookAt(camera.position)
}
