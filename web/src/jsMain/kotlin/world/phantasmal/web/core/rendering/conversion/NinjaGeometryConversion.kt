package world.phantasmal.web.core.rendering.conversion

import mu.KotlinLogging
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint16Array
import world.phantasmal.core.JsArray
import world.phantasmal.core.asArray
import world.phantasmal.core.isBitSet
import world.phantasmal.core.jsArrayOf
import world.phantasmal.core.unsafe.UnsafeMap
import world.phantasmal.psolib.fileFormats.AreaGeometry
import world.phantasmal.psolib.fileFormats.AreaObject
import world.phantasmal.psolib.fileFormats.AreaSection
import world.phantasmal.psolib.fileFormats.CollisionGeometry
import world.phantasmal.psolib.fileFormats.CollisionTriangle
import world.phantasmal.psolib.fileFormats.ninja.NinjaModel
import world.phantasmal.psolib.fileFormats.ninja.NinjaObject
import world.phantasmal.psolib.fileFormats.ninja.NjModel
import world.phantasmal.psolib.fileFormats.ninja.NjObject
import world.phantasmal.psolib.fileFormats.ninja.XjModel
import world.phantasmal.psolib.fileFormats.ninja.XjObject
import world.phantasmal.psolib.fileFormats.ninja.XvrTexture
import world.phantasmal.web.core.dot
import world.phantasmal.web.core.toQuaternion
import world.phantasmal.web.externals.three.Bone
import world.phantasmal.web.externals.three.BufferGeometry
import world.phantasmal.web.externals.three.Color
import world.phantasmal.web.externals.three.DoubleSide
import world.phantasmal.web.externals.three.Euler
import world.phantasmal.web.externals.three.Float32BufferAttribute
import world.phantasmal.web.externals.three.Group
import world.phantasmal.web.externals.three.InstancedMesh
import world.phantasmal.web.externals.three.Material
import world.phantasmal.web.externals.three.Matrix3
import world.phantasmal.web.externals.three.Matrix4
import world.phantasmal.web.externals.three.Mesh
import world.phantasmal.web.externals.three.MeshBasicMaterial
import world.phantasmal.web.externals.three.MeshLambertMaterial
import world.phantasmal.web.externals.three.Quaternion
import world.phantasmal.web.externals.three.SkinnedMesh
import world.phantasmal.web.externals.three.Texture
import world.phantasmal.web.externals.three.Uint16BufferAttribute
import world.phantasmal.web.externals.three.Vector2
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.webui.obj
import kotlin.collections.component1
import kotlin.collections.component2

private val logger = KotlinLogging.logger {}

private val DEFAULT_NORMAL = Vector3(0.0, 1.0, 0.0)
private val DEFAULT_UV = Vector2(0.0, 0.0)
private val NO_TRANSLATION = Vector3(0.0, 0.0, 0.0)
private val NO_ROTATION = Quaternion()
private val NO_SCALE = Vector3(1.0, 1.0, 1.0)

private val COLLISION_MATERIALS: Array<Material> = arrayOf(
    // Wall
    MeshBasicMaterial(obj {
        color = Color(0x80c0d0)
        transparent = true
        opacity = .25
    }),
    // Ground
    MeshLambertMaterial(obj {
        color = Color(0x405050)
        side = DoubleSide
    }),
    // Vegetation
    MeshLambertMaterial(obj {
        color = Color(0x306040)
        side = DoubleSide
    }),
    // Section transition zone
    MeshLambertMaterial(obj {
        color = Color(0x402050)
        side = DoubleSide
    }),
)

private val COLLISION_WIREFRAME_MATERIALS: Array<Material> = arrayOf(
    // Wall
    MeshBasicMaterial(obj {
        color = Color(0x90d0e0)
        wireframe = true
        transparent = true
        opacity = .3
    }),
    // Ground
    MeshBasicMaterial(obj {
        color = Color(0x506060)
        wireframe = true
    }),
    // Vegetation
    MeshBasicMaterial(obj {
        color = Color(0x405050)
        wireframe = true
    }),
    // Section transition zone
    MeshBasicMaterial(obj {
        color = Color(0x503060)
        wireframe = true
    }),
)

// Objects used for temporary calculations to avoid GC.
private val tmpNormal = Vector3()
private val tmpVec = Vector3()
private val tmpNormalMatrix = Matrix3()

class AreaObjectUserData(val sectionId: Int, val areaObject: AreaObject)

fun ninjaObjectToMesh(
    ninjaObject: NinjaObject<*, *>,
    textures: List<XvrTexture?>,
    defaultMaterial: Material? = null,
    boundingVolumes: Boolean = false,
    anisotropy: Int = 1,
): Mesh {
    val builder = MeshBuilder(textures, anisotropy = anisotropy)
    defaultMaterial?.let { builder.defaultMaterial(defaultMaterial) }
    ninjaObjectToMeshBuilder(ninjaObject, builder)
    return builder.buildMesh(boundingVolumes)
}

fun ninjaObjectToInstancedMesh(
    ninjaObject: NinjaObject<*, *>,
    textures: List<XvrTexture>,
    maxInstances: Int,
    defaultMaterial: Material? = null,
    boundingVolumes: Boolean = false,
    anisotropy: Int = 1,
): InstancedMesh {
    val builder = MeshBuilder(textures, anisotropy = anisotropy)
    defaultMaterial?.let { builder.defaultMaterial(defaultMaterial) }
    ninjaObjectToMeshBuilder(ninjaObject, builder)
    return builder.buildInstancedMesh(maxInstances, boundingVolumes)
}

fun ninjaObjectToSkinnedMesh(
    ninjaObject: NjObject,
    textures: List<XvrTexture?>,
    defaultMaterial: Material? = null,
    boundingVolumes: Boolean = false,
    anisotropy: Int = 1,
): SkinnedMesh {
    val builder = MeshBuilder(textures, anisotropy = anisotropy)
    defaultMaterial?.let { builder.defaultMaterial(defaultMaterial) }
    ninjaObjectToMeshBuilder(ninjaObject, builder)
    return builder.buildSkinnedMesh(boundingVolumes)
}

fun ninjaObjectToMeshBuilder(
    ninjaObject: NinjaObject<*, *>,
    builder: MeshBuilder,
) {
    NinjaToMeshConverter(builder).convert(ninjaObject)
}

/** The returned group is not copyable because it contains non-serializable user data. */
fun renderGeometryToGroup(
    renderGeometry: AreaGeometry,
    textures: List<XvrTexture?>,
    anisotropy: Int = 1,
    processMesh: (AreaSection, AreaObject, Mesh) -> Unit = { _, _, _ -> },
): Group {
    val group = Group()
    val textureCache = UnsafeMap<Int, Texture?>()
    val meshCache = UnsafeMap<XjObject, Mesh>()

    for ((sectionIndex, section) in renderGeometry.sections.withIndex()) {
        for (areaObj in section.objects) {
            group.add(
                areaObjectToMesh(
                    textures,
                    textureCache,
                    meshCache,
                    section,
                    sectionIndex,
                    areaObj,
                    anisotropy,
                    processMesh,
                )
            )
        }

        for (areaObj in section.animatedObjects) {
            group.add(
                areaObjectToMesh(
                    textures,
                    textureCache,
                    meshCache,
                    section,
                    sectionIndex,
                    areaObj,
                    anisotropy,
                    processMesh,
                )
            )
        }
    }

    return group
}

/**
 * Calculates a fingerprint that can be used to match duplicated [AreaObject]s across sections, area
 * variants and even areas.
 */
fun AreaObject.fingerPrint(): String =
    buildString {
        append(if (this@fingerPrint is AreaObject.Animated) 'a' else 's')

        append('_')

        var evalFlags = 0
        var childCount = 0
        var vertCount = 0
        var meshCount = 0
        var radius = 0f

        fun recurse(xjObject: XjObject) {
            evalFlags = evalFlags or xjObject.evaluationFlags.bits
            childCount += xjObject.children.size
            vertCount += xjObject.model?.vertices?.size ?: 0
            meshCount += xjObject.model?.meshes?.size ?: 0
            radius += xjObject.model?.collisionSphereRadius ?: 0f
            xjObject.children.forEach(::recurse)
        }

        recurse(xjObject)

        append(evalFlags.toString(36))
        append('_')
        append(childCount.toString(36))
        append('_')
        append(vertCount.toString(36))
        append('_')
        append(meshCount.toString(36))
        append('_')
        append(radius.toRawBits().toUInt().toString(36))
    }

/** The returned mesh is not copyable because it contains non-serializable user data. */
private fun areaObjectToMesh(
    textures: List<XvrTexture?>,
    textureCache: UnsafeMap<Int, Texture?>,
    meshCache: UnsafeMap<XjObject, Mesh>,
    section: AreaSection,
    sectionIndex: Int,
    areaObj: AreaObject,
    anisotropy: Int,
    processMesh: (AreaSection, AreaObject, Mesh) -> Unit,
): Mesh {
    val cachedMesh = meshCache.get(areaObj.xjObject)
    val mesh: Mesh

    if (cachedMesh == null) {
        val builder = MeshBuilder(textures, textureCache, anisotropy)
        ninjaObjectToMeshBuilder(areaObj.xjObject, builder)

        builder.defaultMaterial(MeshLambertMaterial(obj {
            color = Color().setHSL((sectionIndex % 7) / 7.0, 1.0, .5)
            transparent = true
            opacity = .5
            side = DoubleSide
        }))

        mesh = builder.buildMesh(boundingVolumes = true)
        meshCache.set(areaObj.xjObject, mesh)
    } else {
        // If we already have a mesh for this XjObject, make a copy and reuse the existing buffer
        // geometry and materials.
        mesh = Mesh(cachedMesh.geometry, cachedMesh.material.unsafeCast<Array<Material>>())
    }

    mesh.userData = AreaObjectUserData(section.id, areaObj)
    mesh.position.setFromVec3(section.position)
    mesh.rotation.setFromVec3(section.rotation)
    mesh.updateMatrixWorld()

    processMesh(section, areaObj, mesh)

    return mesh
}

fun collisionGeometryToGroup(
    collisionGeometry: CollisionGeometry,
    trianglePredicate: (CollisionTriangle) -> Boolean = { true },
): Group {
    val group = Group()

    for (collisionMesh in collisionGeometry.meshes) {
        val positions = jsArrayOf<Float>()
        val normals = jsArrayOf<Float>()
        val materialGroups = mutableMapOf<Int, JsArray<Short>>()
        var index: Short = 0

        for (triangle in collisionMesh.triangles) {
            // This a vague approximation of the real meaning of these flags.
            val isGround = triangle.flags.isBitSet(0)
            val isVegetation = triangle.flags.isBitSet(4)
            val isSectionTransition = triangle.flags.isBitSet(6)
            val materialIndex = when {
                isSectionTransition -> 3
                isVegetation -> 2
                isGround -> 1
                else -> 0
            }

            if (trianglePredicate(triangle)) {
                val p1 = collisionMesh.vertices[triangle.index1]
                val p2 = collisionMesh.vertices[triangle.index2]
                val p3 = collisionMesh.vertices[triangle.index3]
                positions.push(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, p3.x, p3.y, p3.z)

                val n = triangle.normal
                normals.push(n.x, n.y, n.z, n.x, n.y, n.z, n.x, n.y, n.z)

                val indices = materialGroups.getOrPut(materialIndex) { jsArrayOf() }
                indices.push(index++, index++, index++)
            }
        }

        if (index > 0) {
            val geom = BufferGeometry()
            geom.setAttribute(
                "position", Float32BufferAttribute(Float32Array(positions.asArray()), 3),
            )
            geom.setAttribute(
                "normal", Float32BufferAttribute(Float32Array(normals.asArray()), 3),
            )
            val indices = Uint16Array(index.toInt())
            var offset = 0

            for ((materialIndex, vertexIndices) in materialGroups) {
                indices.set(vertexIndices.asArray(), offset)
                geom.addGroup(offset, vertexIndices.length, materialIndex)
                offset += vertexIndices.length
            }

            geom.setIndex(Uint16BufferAttribute(indices, 1))
            geom.computeBoundingBox()
            geom.computeBoundingSphere()

            group.add(
                Mesh(geom, COLLISION_MATERIALS).apply {
                    renderOrder = 1
                }
            )

            group.add(
                Mesh(geom, COLLISION_WIREFRAME_MATERIALS).apply {
                    renderOrder = 2
                }
            )
        }
    }

    return group
}

// TODO: take into account different kinds of meshes/vertices (with or without normals, uv, etc.).
private class NinjaToMeshConverter(private val builder: MeshBuilder) {
    private val vertexHolder = VertexHolder()
    private var boneIndex = 0

    fun convert(ninjaObject: NinjaObject<*, *>) {
        convertObject(ninjaObject, null, Matrix4())
    }

    private fun convertObject(obj: NinjaObject<*, *>, parentBone: Bone?, parentMatrix: Matrix4) {
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

        val bone: Bone?

        if (ef.skip) {
            bone = parentBone
        } else {
            bone = Bone()
            bone.name = boneIndex.toString()

            bone.position.setFromVec3(obj.position)
            bone.setRotationFromEuler(euler)
            bone.scale.setFromVec3(obj.scale)

            builder.bone(bone)

            parentBone?.add(bone)
        }

        if (!ef.hidden) {
            obj.model?.let { model ->
                convertModel(model, matrix)
            }
        }

        if (!ef.skip) {
            boneIndex++
        }

        if (!ef.breakChildTrace) {
            obj.children.forEach { child ->
                convertObject(child, bone, matrix)
            }
        }
    }

    private fun convertModel(model: NinjaModel, matrix: Matrix4) =
        when (model) {
            is NjModel -> convertNjModel(model, matrix)
            is XjModel -> convertXjModel(model, matrix)
        }

    private fun convertNjModel(model: NjModel, matrix: Matrix4) {
        tmpNormalMatrix.getNormalMatrix(matrix)

        val newVertices = model.vertices.map { vertex ->
            vertex?.let {
                val position = vec3ToThree(vertex.position)
                val normal = vertex.normal?.let(::vec3ToThree) ?: Vector3(0.0, 1.0, 0.0)

                position.applyMatrix4(matrix)
                normal.applyMatrix3(tmpNormalMatrix)

                Vertex(
                    boneIndex,
                    position,
                    normal,
                    vertex.boneWeight,
                    vertex.boneWeightStatus,
                )
            }
        }

        vertexHolder.add(newVertices)

        for (mesh in model.meshes) {
            val group = builder.getGroupIndex(
                mesh.textureId,
                alpha = mesh.useAlpha,
                additiveBlending = mesh.srcAlpha != 4 || mesh.dstAlpha != 5,
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

                    val boneIndices = IntArray(4)
                    val boneWeights = FloatArray(4)

                    if (vertex.boneWeight == null) {
                        boneIndices[0] = vertex.boneIndex
                        boneWeights[0] = 1f
                    } else {
                        for (v in vertices) {
                            boneIndices[v.boneWeightStatus] = v.boneIndex
                            boneWeights[v.boneWeightStatus] = v.boneWeight ?: 1f
                        }
                    }

                    val totalWeight = boneWeights.sum()

                    if (totalWeight > 0f) {
                        val weightFactor = 1f / totalWeight

                        for (j in boneWeights.indices) {
                            boneWeights[j] *= weightFactor
                        }
                    }

                    builder.vertex(
                        vertex.position,
                        normal,
                        meshVertex.texCoords?.let(::vec2ToThree) ?: DEFAULT_UV,
                        boneIndices,
                        boneWeights,
                    )

                    if (i >= 2) {
                        if (i % 2 == if (mesh.clockwiseWinding) 1 else 0) {
                            builder.index(group, index - 2)
                            builder.index(group, index - 1)
                            builder.index(group, index)
                        } else {
                            builder.index(group, index - 2)
                            builder.index(group, index)
                            builder.index(group, index - 1)
                        }
                    }

                    i++
                }
            }
        }
    }

    private fun convertXjModel(model: XjModel, matrix: Matrix4) {
        val indexOffset = builder.vertexCount
        tmpNormalMatrix.getNormalMatrix(matrix)

        for (vertex in model.vertices) {
            val p = vec3ToThree(vertex.position)
            p.applyMatrix4(matrix)

            val n = vertex.normal?.let(::vec3ToThree) ?: Vector3(0.0, 1.0, 0.0)
            n.applyMatrix3(tmpNormalMatrix)

            val uv = vertex.uv?.let(::vec2ToThree) ?: DEFAULT_UV

            builder.vertex(p, n, uv)
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
                tmpNormal.copy(pb)
                tmpNormal.sub(pa)
                tmpVec.copy(pc)
                tmpVec.sub(pa)
                tmpNormal.cross(tmpVec)

                if (clockwise) {
                    tmpNormal.negate()
                }

                val oppositeCount =
                    (if (tmpNormal dot na < 0) 1 else 0) +
                            (if (tmpNormal dot nb < 0) 1 else 0) +
                            (if (tmpNormal dot nc < 0) 1 else 0)

                if (oppositeCount >= 2) {
                    clockwise = !clockwise
                }

                if (clockwise) {
                    builder.index(group, b)
                    builder.index(group, a)
                    builder.index(group, c)
                } else {
                    builder.index(group, a)
                    builder.index(group, b)
                    builder.index(group, c)
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
    val boneWeight: Float?,
    val boneWeightStatus: Int,
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
