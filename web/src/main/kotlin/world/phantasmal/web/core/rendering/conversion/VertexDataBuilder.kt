package world.phantasmal.web.core.rendering.conversion

import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint16Array
import org.khronos.webgl.set
import world.phantasmal.web.externals.babylon.Vector2
import world.phantasmal.web.externals.babylon.Vector3
import world.phantasmal.web.externals.babylon.VertexData

class VertexDataBuilder {
    private val positions = mutableListOf<Vector3>()
    private val normals = mutableListOf<Vector3>()
    private val uvs = mutableListOf<Vector2>()
    private val indices = mutableListOf<Short>()
    private val boneIndices = mutableListOf<Short>()
    private val boneWeights = mutableListOf<Float>()

    val vertexCount: Int
        get() = positions.size

    val indexCount: Int
        get() = indices.size

    fun getPosition(index: Int): Vector3 =
        positions[index]

    fun getNormal(index: Int): Vector3 =
        normals[index]

    fun addVertex(position: Vector3, normal: Vector3, uv: Vector2? = null) {
        positions.add(position)
        normals.add(normal)
        uv?.let { uvs.add(uv) }
    }

    fun addIndex(index: Int) {
        indices.add(index.toShort())
    }

    fun addBoneWeight(index: Int, weight: Float) {
        boneIndices.add(index.toShort())
        boneWeights.add(weight)
    }

    // TODO: support multiple materials
//    fun addGroup(
//        offset: Int,
//        size: Int,
//        textureId: Int?,
//        alpha: Boolean = false,
//        additiveBlending: Boolean = false,
//    ) {
//
//    }

    fun build(): VertexData {
        check(this.positions.size == this.normals.size)
        check(this.uvs.isEmpty() || this.positions.size == this.uvs.size)

        val positions = Float32Array(3 * positions.size)
        val normals = Float32Array(3 * normals.size)
        val uvs = if (uvs.isEmpty()) null else Float32Array(2 * uvs.size)

        for (i in this.positions.indices) {
            val pos = this.positions[i]
            positions[3 * i] = pos.x.toFloat()
            positions[3 * i + 1] = pos.y.toFloat()
            positions[3 * i + 2] = pos.z.toFloat()

            val normal = this.normals[i]
            normals[3 * i] = normal.x.toFloat()
            normals[3 * i + 1] = normal.y.toFloat()
            normals[3 * i + 2] = normal.z.toFloat()

            uvs?.let {
                val uv = this.uvs[i]
                uvs[2 * i] = uv.x.toFloat()
                uvs[2 * i + 1] = uv.y.toFloat()
            }
        }

        val data = VertexData()
        data.positions = positions
        data.normals = normals
        data.uvs = uvs
        data.indices = Uint16Array(indices.toTypedArray())
        return data
    }
}
