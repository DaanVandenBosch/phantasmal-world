package world.phantasmal.web.core.rendering.conversion

import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint16Array
import org.khronos.webgl.set
import world.phantasmal.lib.fileFormats.ninja.XvrTexture
import world.phantasmal.web.externals.three.*
import world.phantasmal.webui.obj

class MeshBuilder {
    private val positions = mutableListOf<Vector3>()
    private val normals = mutableListOf<Vector3>()
    private val uvs = mutableListOf<Vector2>()

    /**
     * One group per material.
     */
    private val groups = mutableListOf<Group>()

    private var defaultMaterial: Material = MeshLambertMaterial(obj {
        // TODO: skinning
        side = DoubleSide
    })

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
        groups[groupIdx].indices.add(index.toShort())
    }

    fun boneWeight(groupIdx: Int, index: Int, weight: Float) {
        val group = groups[groupIdx]
        group.boneIndices.add(index.toShort())
        group.boneWeights.add(weight)
    }

    fun defaultMaterial(material: Material) {
        defaultMaterial = material
    }

    fun textures(textures: List<XvrTexture?>) {
        this.textures.addAll(textures)
    }

    fun buildMesh(boundingVolumes: Boolean = false): Mesh =
        build().let { (geom, materials) ->
            if (boundingVolumes) {
                geom.computeBoundingBox()
                geom.computeBoundingSphere()
            }

            Mesh(geom, materials)
        }

    /**
     * Creates an [InstancedMesh] with 0 instances.
     */
    fun buildInstancedMesh(maxInstances: Int, boundingVolumes: Boolean = false): InstancedMesh =
        build().let { (geom, materials) ->
            if (boundingVolumes) {
                geom.computeBoundingBox()
                geom.computeBoundingSphere()
            }

            InstancedMesh(geom, materials, maxInstances).apply {
                // Start with 0 instances.
                count = 0
            }
        }

    private fun build(): Pair<BufferGeometry, Array<Material>> {
        check(positions.size == normals.size)
        check(uvs.isEmpty() || positions.size == uvs.size)

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
        val indices = Uint16Array(groups.sumBy { it.indices.size })

        var offset = 0
        val texCache = mutableMapOf<Int, Texture?>()

        val materials = mutableListOf<Material>()

        for (group in groups) {
            indices.set(group.indices.toTypedArray(), offset)
            geom.addGroup(offset, group.indices.size, materials.size)

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
                    // TODO: skinning
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
            offset += group.indices.size
        }

        geom.setIndex(Uint16BufferAttribute(indices, 1))

        return Pair(geom, materials.toTypedArray())
    }

    private class Group(
        val textureId: Int?,
        val alpha: Boolean,
        val additiveBlending: Boolean,
    ) {
        val indices = mutableListOf<Short>()
        val boneIndices = mutableListOf<Short>()
        val boneWeights = mutableListOf<Float>()
    }
}
