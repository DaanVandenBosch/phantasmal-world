package world.phantasmal.web.core.rendering.conversion

import mu.KotlinLogging
import world.phantasmal.lib.fileFormats.Vec3
import world.phantasmal.lib.fileFormats.ninja.NinjaModel
import world.phantasmal.lib.fileFormats.ninja.NinjaObject
import world.phantasmal.lib.fileFormats.ninja.NjModel
import world.phantasmal.lib.fileFormats.ninja.XjModel
import world.phantasmal.web.core.*
import world.phantasmal.web.externals.babylon.*
import kotlin.math.cos
import kotlin.math.sin

private val logger = KotlinLogging.logger {}

private val DEFAULT_NORMAL = Vector3.Up()
private val DEFAULT_UV = Vector2.Zero()
private val NO_TRANSLATION = Vector3.Zero()
private val NO_ROTATION = Quaternion.Identity()
private val NO_SCALE = Vector3.One()

fun ninjaObjectToVertexData(ninjaObject: NinjaObject<*>): VertexData =
    NinjaToVertexDataConverter(VertexDataBuilder()).convert(ninjaObject)

fun ninjaObjectToVertexDataBuilder(
    ninjaObject: NinjaObject<*>,
    builder: VertexDataBuilder,
): VertexData =
    NinjaToVertexDataConverter(builder).convert(ninjaObject)

// TODO: take into account different kinds of meshes/vertices (with or without normals, uv, etc.).
private class NinjaToVertexDataConverter(private val builder: VertexDataBuilder) {
    private val vertexHolder = VertexHolder()
    private var boneIndex = 0

    fun convert(ninjaObject: NinjaObject<*>): VertexData {
        objectToVertexData(ninjaObject, Matrix.Identity())
        return builder.build()
    }

    private fun objectToVertexData(obj: NinjaObject<*>, parentMatrix: Matrix) {
        val ef = obj.evaluationFlags

        val matrix = Matrix.Compose(
            if (ef.noScale) NO_SCALE else vec3ToBabylon(obj.scale),
            if (ef.noRotate) NO_ROTATION else eulerToQuat(obj.rotation, ef.zxyRotationOrder),
            if (ef.noTranslate) NO_TRANSLATION else vec3ToBabylon(obj.position),
        )
        matrix.preMultiply(parentMatrix)

        if (!ef.hidden) {
            obj.model?.let { model ->
                modelToVertexData(model, matrix)
            }
        }

        boneIndex++

        if (!ef.breakChildTrace) {
            obj.children.forEach { child ->
                objectToVertexData(child, matrix)
            }
        }
    }

    private fun modelToVertexData(model: NinjaModel, matrix: Matrix) =
        when (model) {
            is NjModel -> njModelToVertexData(model, matrix)
            is XjModel -> xjModelToVertexData(model, matrix)
        }

    private fun njModelToVertexData(model: NjModel, matrix: Matrix) {
        val normalMatrix = Matrix.Identity()
        matrix.toNormalMatrix(normalMatrix)

        val newVertices = model.vertices.map { vertex ->
            vertex?.let {
                val position = vec3ToBabylon(vertex.position)
                val normal = vertex.normal?.let(::vec3ToBabylon) ?: Vector3.Up()

                matrix.multiply(position)
                normalMatrix.multiply3x3(normal)

                Vertex(
                    boneIndex,
                    position,
                    normal,
                    vertex.boneWeight,
                    vertex.boneWeightStatus,
                    vertex.calcContinue,
                )
            }
        }

        vertexHolder.add(newVertices)

        for (mesh in model.meshes) {
            val startIndexCount = builder.indexCount
            var i = 0

            for (meshVertex in mesh.vertices) {
                val vertices = vertexHolder.get(meshVertex.index)

                if (vertices.isEmpty()) {
                    logger.debug {
                        "Mesh refers to nonexistent vertex with index ${meshVertex.index}."
                    }
                } else {
                    val vertex = vertices.last()
                    val normal =
                        vertex.normal ?: meshVertex.normal?.let(::vec3ToBabylon) ?: DEFAULT_NORMAL
                    val index = builder.vertexCount

                    builder.addVertex(
                        vertex.position,
                        normal,
                        meshVertex.texCoords?.let(::vec2ToBabylon) ?: DEFAULT_UV
                    )

                    if (i >= 2) {
                        if (i % 2 == if (mesh.clockwiseWinding) 0 else 1) {
                            builder.addIndex(index - 2)
                            builder.addIndex(index - 1)
                            builder.addIndex(index)
                        } else {
                            builder.addIndex(index - 2)
                            builder.addIndex(index)
                            builder.addIndex(index - 1)
                        }
                    }

                    val boneIndices = IntArray(4)
                    val boneWeights = FloatArray(4)

                    for (v in vertices) {
                        boneIndices[v.boneWeightStatus] = v.boneIndex
                        boneWeights[v.boneWeightStatus] = v.boneWeight
                    }

                    val totalWeight = boneWeights.sum()

                    for (j in boneIndices.indices) {
                        builder.addBoneWeight(
                            boneIndices[j],
                            if (totalWeight > 0f) boneWeights[j] / totalWeight else 0f
                        )
                    }

                    i++
                }
            }

            // TODO: support multiple materials
//            builder.addGroup(
//                startIndexCount
//            )
        }
    }

    private fun xjModelToVertexData(model: XjModel, matrix: Matrix) {
        val indexOffset = builder.vertexCount
        val normalMatrix = Matrix.Identity()
        matrix.toNormalMatrix(normalMatrix)

        for (vertex in model.vertices) {
            val p = vec3ToBabylon(vertex.position)
            matrix.multiply(p)

            val n = vertex.normal?.let(::vec3ToBabylon) ?: Vector3.Up()
            normalMatrix.multiply3x3(n)

            val uv = vertex.uv?.let(::vec2ToBabylon) ?: DEFAULT_UV

            builder.addVertex(p, n, uv)
        }

        var currentMatIdx: Int? = null
        var currentSrcAlpha: Int? = null
        var currentDstAlpha: Int? = null

        for (mesh in model.meshes) {
            val startIndexCount = builder.indexCount
            var clockwise = true

            for (j in 2 until mesh.indices.size) {
                val a = indexOffset + mesh.indices[j - 2]
                val b = indexOffset + mesh.indices[j - 1]
                val c = indexOffset + mesh.indices[j]
                val pa = builder.getPosition(a)
                val pb = builder.getPosition(b)
                val pc = builder.getPosition(c)
                val na = builder.getNormal(a)
                val nb = builder.getNormal(b)
                val nc = builder.getNormal(c)

                // Calculate a surface normal and reverse the vertex winding if at least 2 of the
                // vertex normals point in the opposite direction. This hack fixes the winding for
                // most models.
                val normal = (pb - pa) cross (pc - pa)

                if (!clockwise) {
                    normal.negateInPlace()
                }

                val oppositeCount =
                    (if (normal dot na < 0) 1 else 0) +
                            (if (normal dot nb < 0) 1 else 0) +
                            (if (normal dot nc < 0) 1 else 0)

                if (oppositeCount >= 2) {
                    clockwise = !clockwise
                }

                if (clockwise) {
                    builder.addIndex(b)
                    builder.addIndex(a)
                    builder.addIndex(c)
                } else {
                    builder.addIndex(a)
                    builder.addIndex(b)
                    builder.addIndex(c)
                }

                clockwise = !clockwise
            }

            mesh.material.textureId?.let { currentMatIdx = it }
            mesh.material.srcAlpha?.let { currentSrcAlpha = it }
            mesh.material.dstAlpha?.let { currentDstAlpha = it }

            // TODO: support multiple materials
//            builder.addGroup(
//                start_index_count,
//                this.builder.index_count - start_index_count,
//                current_mat_idx,
//                true,
//                current_src_alpha !== 4 || current_dst_alpha !== 5,
//            );
        }
    }
}

private class Vertex(
    val boneIndex: Int,
    val position: Vector3,
    val normal: Vector3?,
    val boneWeight: Float,
    val boneWeightStatus: Int,
    val calcContinue: Boolean,
)

private class VertexHolder {
    private val buffer = mutableListOf<MutableList<Vertex>>()

    fun add(vertices: List<Vertex?>) {
        vertices.forEachIndexed { i, vertex ->
            if (i >= buffer.size) {
                buffer.add(mutableListOf())
            }

            if (vertex != null) {
                buffer[i].add(vertex)
            }
        }
    }

    fun get(index: Int): List<Vertex> = buffer[index]
}

private fun eulerToQuat(angles: Vec3, zxyRotationOrder: Boolean): Quaternion {
    val x = angles.x.toDouble()
    val y = angles.y.toDouble()
    val z = angles.z.toDouble()

    val c1 = cos(x / 2)
    val c2 = cos(y / 2)
    val c3 = cos(z / 2)

    val s1 = sin(x / 2)
    val s2 = sin(y / 2)
    val s3 = sin(z / 2)

    return if (zxyRotationOrder) {
        Quaternion(
            s1 * c2 * c3 - c1 * s2 * s3,
            c1 * s2 * c3 + s1 * c2 * s3,
            c1 * c2 * s3 + s1 * s2 * c3,
            c1 * c2 * c3 - s1 * s2 * s3,
        )
    } else {
        Quaternion(
            s1 * c2 * c3 - c1 * s2 * s3,
            c1 * s2 * c3 + s1 * c2 * s3,
            c1 * c2 * s3 - s1 * s2 * c3,
            c1 * c2 * c3 + s1 * s2 * s3,
        )
    }
}
