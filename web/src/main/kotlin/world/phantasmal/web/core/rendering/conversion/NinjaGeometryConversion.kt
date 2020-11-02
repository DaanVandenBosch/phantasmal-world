package world.phantasmal.web.core.rendering.conversion

import mu.KotlinLogging
import world.phantasmal.lib.fileFormats.Vec3
import world.phantasmal.lib.fileFormats.ninja.NinjaModel
import world.phantasmal.lib.fileFormats.ninja.NinjaObject
import world.phantasmal.lib.fileFormats.ninja.NjcmModel
import world.phantasmal.lib.fileFormats.ninja.XjModel
import world.phantasmal.web.externals.babylon.*
import kotlin.math.cos
import kotlin.math.sin

private val logger = KotlinLogging.logger {}

private val DEFAULT_NORMAL = Vector3.Up()
private val DEFAULT_UV = Vector2.Zero()
private val NO_TRANSLATION = Vector3.Zero()
private val NO_ROTATION = Quaternion.Identity()
private val NO_SCALE = Vector3.One()

// TODO: take into account different kinds of meshes/vertices (with or without normals, uv, etc.).
fun ninjaObjectToVertexData(ninjaObject: NinjaObject<*>): VertexData =
    NinjaToVertexDataConverter(VertexDataBuilder()).convert(ninjaObject)

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

        parentMatrix.multiplyToRef(matrix, matrix)

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
            is NjcmModel -> njcmModelToVertexData(model, matrix)
            is XjModel -> xjModelToVertexData(model, matrix)
        }

    private fun njcmModelToVertexData(model: NjcmModel, matrix: Matrix) {
        val normalMatrix = Matrix.Identity()
        matrix.toNormalMatrix(normalMatrix)

        val newVertices = model.vertices.map { vertex ->
            vertex?.let {
                val position = vec3ToBabylon(vertex.position)
                val normal = vertex.normal?.let(::vec3ToBabylon) ?: Vector3.Up()

                Vector3.TransformCoordinatesToRef(position, matrix, position)
                Vector3.TransformNormalToRef(normal, normalMatrix, normal)

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
                val vertices = vertexHolder.get(meshVertex.index.toInt())

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
                        if (i % 2 == if (mesh.clockwiseWinding) 1 else 0) {
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

    private fun xjModelToVertexData(model: XjModel, matrix: Matrix) {}
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
    private val stack = mutableListOf<MutableList<Vertex>>()

    fun add(vertices: List<Vertex?>) {
        vertices.forEachIndexed { i, vertex ->
            if (i >= stack.size) {
                stack.add(mutableListOf())
            }

            if (vertex != null) {
                stack[i].add(vertex)
            }
        }
    }

    fun get(index: Int): List<Vertex> = stack[index]
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
