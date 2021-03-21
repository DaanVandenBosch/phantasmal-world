package world.phantasmal.web.core.rendering.conversion

import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint16Array
import org.khronos.webgl.set
import world.phantasmal.core.asArray
import world.phantasmal.core.jsArrayOf
import world.phantasmal.lib.fileFormats.ninja.XvrTexture
import world.phantasmal.web.externals.three.*
import world.phantasmal.webui.obj

class MeshBuilder {
    private val positions = mutableListOf<Vector3>()
    private val normals = mutableListOf<Vector3>()
    private val uvs = mutableListOf<Vector2>()
    private val boneIndices = mutableListOf<Short>()
    private val boneWeights = mutableListOf<Float>()
    private val bones = mutableListOf<Bone>()

    /**
     * One group per material.
     */
    private val groups = mutableListOf<Group>()
    private var indexCount = 0

    private var defaultMaterial: Material? = null

    private val textures = mutableListOf<XvrTexture?>()

    fun getGroupIndex(
        textureId: Int?,
        alpha: Boolean,
        additiveBlending: Boolean,
    ): Int {
        val idx = groups.indexOfFirst {
            it.textureId == textureId &&
                    it.alpha == alpha &&
                    it.additiveBlending == additiveBlending
        }

        return if (idx != -1) {
            idx
        } else {
            groups.add(Group(textureId, alpha, additiveBlending))
            groups.lastIndex
        }
    }

    val vertexCount: Int
        get() = positions.size

    fun getPosition(index: Int): Vector3 =
        positions[index]

    fun getNormal(index: Int): Vector3 =
        normals[index]

    fun vertex(position: Vector3, normal: Vector3, uv: Vector2? = null) {
        positions.add(position)
        normals.add(normal)
        uv?.let { uvs.add(uv) }
    }

    fun index(groupIdx: Int, index: Int) {
        groups[groupIdx].indices.push(index.toShort())
        indexCount++
    }

    fun bone(bone: Bone) {
        bones.add(bone)
    }

    fun boneWeights(indices: IntArray, weights: FloatArray) {
        require(indices.size == 4)
        require(weights.size == 4)

        for (index in indices) {
            boneIndices.add(index.toShort())
        }

        for (weight in weights) {
            boneWeights.add(weight)
        }
    }

    fun defaultMaterial(material: Material) {
        defaultMaterial = material
    }

    fun textures(textures: List<XvrTexture?>) {
        this.textures.addAll(textures)
    }

    fun buildMesh(boundingVolumes: Boolean = false): Mesh =
        build(skinning = false, boundingVolumes).let { (geom, materials) ->
            Mesh(geom, materials)
        }

    /**
     * Creates an [InstancedMesh] with 0 instances.
     */
    fun buildInstancedMesh(maxInstances: Int, boundingVolumes: Boolean = false): InstancedMesh =
        build(skinning = false, boundingVolumes).let { (geom, materials) ->
            InstancedMesh(geom, materials, maxInstances).apply {
                // Start with 0 instances.
                count = 0
            }
        }

    /**
     * Creates a [SkinnedMesh] with bones and a skeleton for animation.
     */
    fun buildSkinnedMesh(boundingVolumes: Boolean = false): SkinnedMesh =
        build(skinning = true, boundingVolumes).let { (geom, materials, bones) ->
            SkinnedMesh(geom, materials).apply {
                add(bones[0])
                bind(Skeleton(bones))
            }
        }

    private fun build(
        skinning: Boolean,
        boundingVolumes: Boolean,
    ): Triple<BufferGeometry, Array<Material>, Array<Bone>> {
        check(positions.size == normals.size)
        check(uvs.isEmpty() || positions.size == uvs.size)

        // Per-buffer attributes.
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

        val geom = BufferGeometry()
        geom.setAttribute("position", Float32BufferAttribute(positions, 3))
        geom.setAttribute("normal", Float32BufferAttribute(normals, 3))
        uvs?.let { geom.setAttribute("uv", Float32BufferAttribute(uvs, 2)) }

        // Per group/material attributes.
        val indices = Uint16Array(indexCount)

        if (skinning) {
            check(this.positions.size == boneIndices.size / 4)
            check(this.positions.size == boneWeights.size / 4)

            boneIndices.maxOrNull()?.let {
                check(it < bones.size)
            }

            geom.setAttribute(
                "skinIndex",
                Uint16BufferAttribute(Uint16Array(boneIndices.toTypedArray()), 4)
            )
            geom.setAttribute(
                "skinWeight",
                Float32BufferAttribute(Float32Array(boneWeights.toTypedArray()), 4)
            )
        }

        var offset = 0
        val texCache = mutableMapOf<Int, Texture?>()

        val materials = mutableListOf<Material>()

        val defaultMaterial = defaultMaterial ?: MeshLambertMaterial(obj {
            this.skinning = skinning
            side = DoubleSide
        })

        for (group in groups) {
            indices.set(group.indices.asArray(), offset)
            geom.addGroup(offset, group.indices.length, materials.size)

            val tex = group.textureId?.let { texId ->
                texCache.getOrPut(texId) {
                    textures.getOrNull(texId)?.let { xvm ->
                        xvrTextureToThree(xvm)
                    }
                }
            }

            val mat = if (tex == null) {
                defaultMaterial
            } else {
                MeshBasicMaterial(obj {
                    this.skinning = skinning
                    map = tex
                    side = DoubleSide

                    if (group.alpha) {
                        transparent = true
                        alphaTest = 0.01
                    }

                    if (group.additiveBlending) {
                        transparent = true
                        alphaTest = 0.01
                        blending = AdditiveBlending
                    }
                })
            }

            materials.add(mat)
            offset += group.indices.length
        }

        geom.setIndex(Uint16BufferAttribute(indices, 1))

        if (boundingVolumes) {
            geom.computeBoundingBox()
            geom.computeBoundingSphere()
        }

        return Triple(geom, materials.toTypedArray(), bones.toTypedArray())
    }

    private class Group(
        val textureId: Int?,
        val alpha: Boolean,
        val additiveBlending: Boolean,
    ) {
        val indices = jsArrayOf<Short>()
    }
}
