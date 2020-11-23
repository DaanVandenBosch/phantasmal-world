package world.phantasmal.web.core.rendering.conversion

import mu.KotlinLogging
import world.phantasmal.lib.fileFormats.ninja.*
import world.phantasmal.web.core.cross
import world.phantasmal.web.core.dot
import world.phantasmal.web.core.minus
import world.phantasmal.web.core.toQuaternion
import world.phantasmal.web.externals.three.*

private val logger = KotlinLogging.logger {}

private val DEFAULT_NORMAL = Vector3(0.0, 1.0, 0.0)
private val DEFAULT_UV = Vector2(0.0, 0.0)
private val NO_TRANSLATION = Vector3(0.0, 0.0, 0.0)
private val NO_ROTATION = Quaternion()
private val NO_SCALE = Vector3(1.0, 1.0, 1.0)

fun ninjaObjectToMesh(
    ninjaObject: NinjaObject<*>,
    textures: List<XvrTexture>,
    boundingVolumes: Boolean = false
): Mesh {
    val builder = MeshBuilder()
    builder.addTextures(textures)
    NinjaToMeshConverter(builder).convert(ninjaObject)
    return builder.buildMesh(boundingVolumes)
}

fun ninjaObjectToInstancedMesh(
    ninjaObject: NinjaObject<*>,
    textures: List<XvrTexture>,
    maxInstances: Int,
    boundingVolumes: Boolean = false,
): InstancedMesh {
    val builder = MeshBuilder()
    builder.addTextures(textures)
    NinjaToMeshConverter(builder).convert(ninjaObject)
    return builder.buildInstancedMesh(maxInstances, boundingVolumes)
}

fun ninjaObjectToMeshBuilder(
    ninjaObject: NinjaObject<*>,
    builder: MeshBuilder,
) {
    NinjaToMeshConverter(builder).convert(ninjaObject)
}

// TODO: take into account different kinds of meshes/vertices (with or without normals, uv, etc.).
private class NinjaToMeshConverter(private val builder: MeshBuilder) {
    private val vertexHolder = VertexHolder()
    private var boneIndex = 0

    fun convert(ninjaObject: NinjaObject<*>) {
        convertObject(ninjaObject, Matrix4())
    }

    private fun convertObject(obj: NinjaObject<*>, parentMatrix: Matrix4) {
        val ef = obj.evaluationFlags

        val euler = Euler(
            obj.rotation.x.toDouble(),
            obj.rotation.y.toDouble(),
            obj.rotation.z.toDouble(),
            if (ef.zxyRotationOrder) "ZXY" else "ZYX",
        )
        val matrix = Matrix4()
            .compose(
                if (ef.noTranslate) NO_TRANSLATION else vec3ToThree(obj.position),
                if (ef.noRotate) NO_ROTATION else euler.toQuaternion(),
                if (ef.noScale) NO_SCALE else vec3ToThree(obj.scale),
            )
            .premultiply(parentMatrix)

        if (!ef.hidden) {
            obj.model?.let { model ->
                convertModel(model, matrix)
            }
        }

        boneIndex++

        if (!ef.breakChildTrace) {
            obj.children.forEach { child ->
                convertObject(child, matrix)
            }
        }
    }

    private fun convertModel(model: NinjaModel, matrix: Matrix4) =
        when (model) {
            is NjModel -> convertNjModel(model, matrix)
            is XjModel -> convertXjModel(model, matrix)
        }

    private fun convertNjModel(model: NjModel, matrix: Matrix4) {
        val normalMatrix = Matrix3().getNormalMatrix(matrix)

        val newVertices = model.vertices.map { vertex ->
            vertex?.let {
                val position = vec3ToThree(vertex.position)
                val normal = vertex.normal?.let(::vec3ToThree) ?: Vector3(0.0, 1.0, 0.0)

                position.applyMatrix4(matrix)
                normal.applyMatrix3(normalMatrix)

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
            val group = builder.getGroupIndex(
                mesh.textureId,
                alpha = mesh.useAlpha,
                additiveBlending = mesh.srcAlpha != 4 || mesh.dstAlpha != 5
            )
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
                        vertex.normal ?: meshVertex.normal?.let(::vec3ToThree) ?: DEFAULT_NORMAL
                    val index = builder.vertexCount

                    builder.addVertex(
                        vertex.position,
                        normal,
                        meshVertex.texCoords?.let(::vec2ToThree) ?: DEFAULT_UV
                    )

                    if (i >= 2) {
                        if (i % 2 == if (mesh.clockwiseWinding) 1 else 0) {
                            builder.addIndex(group, index - 2)
                            builder.addIndex(group, index - 1)
                            builder.addIndex(group, index)
                        } else {
                            builder.addIndex(group, index - 2)
                            builder.addIndex(group, index)
                            builder.addIndex(group, index - 1)
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
                            group,
                            boneIndices[j],
                            if (totalWeight > 0f) boneWeights[j] / totalWeight else 0f
                        )
                    }

                    i++
                }
            }
        }
    }

    private fun convertXjModel(model: XjModel, matrix: Matrix4) {
        val indexOffset = builder.vertexCount
        val normalMatrix = Matrix3().getNormalMatrix(matrix)

        for (vertex in model.vertices) {
            val p = vec3ToThree(vertex.position)
            p.applyMatrix4(matrix)

            val n = vertex.normal?.let(::vec3ToThree) ?: Vector3(0.0, 1.0, 0.0)
            n.applyMatrix3(normalMatrix)

            val uv = vertex.uv?.let(::vec2ToThree) ?: DEFAULT_UV

            builder.addVertex(p, n, uv)
        }

        var currentTextureIdx: Int? = null
        var currentSrcAlpha: Int? = null
        var currentDstAlpha: Int? = null

        for (mesh in model.meshes) {
            mesh.material.textureId?.let { currentTextureIdx = it }
            mesh.material.srcAlpha?.let { currentSrcAlpha = it }
            mesh.material.dstAlpha?.let { currentDstAlpha = it }

            val group = builder.getGroupIndex(
                currentTextureIdx,
                alpha = true,
                additiveBlending = currentSrcAlpha != 4 || currentDstAlpha != 5,
            )

            var clockwise = false

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

                if (clockwise) {
                    normal.negate()
                }

                val oppositeCount =
                    (if (normal dot na < 0) 1 else 0) +
                            (if (normal dot nb < 0) 1 else 0) +
                            (if (normal dot nc < 0) 1 else 0)

                if (oppositeCount >= 2) {
                    clockwise = !clockwise
                }

                if (clockwise) {
                    builder.addIndex(group, b)
                    builder.addIndex(group, a)
                    builder.addIndex(group, c)
                } else {
                    builder.addIndex(group, a)
                    builder.addIndex(group, b)
                    builder.addIndex(group, c)
                }

                clockwise = !clockwise
            }
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
