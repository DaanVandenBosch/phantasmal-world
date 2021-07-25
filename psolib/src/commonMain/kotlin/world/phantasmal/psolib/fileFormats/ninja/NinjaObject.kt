package world.phantasmal.psolib.fileFormats.ninja

import world.phantasmal.core.isBitSet
import world.phantasmal.core.setBit
import world.phantasmal.psolib.fileFormats.Vec2
import world.phantasmal.psolib.fileFormats.Vec3

sealed class NinjaObject<Model : NinjaModel, Self : NinjaObject<Model, Self>>(
    val offset: Int,
    val evaluationFlags: NinjaEvaluationFlags,
    val model: Model?,
    val position: Vec3,
    /**
     * Euler angles in radians.
     */
    val rotation: Vec3,
    val scale: Vec3,
    children: MutableList<Self>,
) {
    private val _children = children
    val children: List<Self> = _children

    fun addChild(child: Self) {
        _children.add(child)
    }

    fun boneCount(): Int {
        val indexRef = intArrayOf(0)
        @Suppress("UNCHECKED_CAST")
        findBone(this as Self, Int.MAX_VALUE, indexRef)
        return indexRef[0]
    }

    fun getBone(index: Int): Self? =
        @Suppress("UNCHECKED_CAST")
        findBone(this as Self, index, intArrayOf(0))

    private fun findBone(
        obj: Self,
        boneIndex: Int,
        indexRef: IntArray,
    ): Self? {
        if (!obj.evaluationFlags.skip) {
            val index = indexRef[0]++

            if (index == boneIndex) {
                return obj
            }
        }

        if (!obj.evaluationFlags.breakChildTrace) {
            for (child in obj.children) {
                val bone = findBone(child, boneIndex, indexRef)
                if (bone != null) return bone
            }
        }

        return null
    }
}

class NjObject(
    offset: Int,
    evaluationFlags: NinjaEvaluationFlags,
    model: NjModel?,
    position: Vec3,
    rotation: Vec3,
    scale: Vec3,
    children: MutableList<NjObject>,
) : NinjaObject<NjModel, NjObject>(
    offset,
    evaluationFlags,
    model,
    position,
    rotation,
    scale,
    children,
)

class XjObject(
    offset: Int,
    evaluationFlags: NinjaEvaluationFlags,
    model: XjModel?,
    position: Vec3,
    rotation: Vec3,
    scale: Vec3,
    children: MutableList<XjObject>,
) : NinjaObject<XjModel, XjObject>(
    offset,
    evaluationFlags,
    model,
    position,
    rotation,
    scale,
    children,
)

class NinjaEvaluationFlags(bits: Int) {
    var bits: Int = bits
        private set
    var noTranslate: Boolean
        get() = bits.isBitSet(0)
        set(value) {
            bits = bits.setBit(0, value)
        }
    var noRotate: Boolean
        get() = bits.isBitSet(1)
        set(value) {
            bits = bits.setBit(1, value)
        }
    var noScale: Boolean
        get() = bits.isBitSet(2)
        set(value) {
            bits = bits.setBit(2, value)
        }
    var hidden: Boolean
        get() = bits.isBitSet(3)
        set(value) {
            bits = bits.setBit(3, value)
        }
    var breakChildTrace: Boolean
        get() = bits.isBitSet(4)
        set(value) {
            bits = bits.setBit(4, value)
        }
    var zxyRotationOrder: Boolean
        get() = bits.isBitSet(5)
        set(value) {
            bits = bits.setBit(5, value)
        }
    var skip: Boolean
        get() = bits.isBitSet(6)
        set(value) {
            bits = bits.setBit(6, value)
        }
    var shapeSkip: Boolean
        get() = bits.isBitSet(7)
        set(value) {
            bits = bits.setBit(7, value)
        }
    var clip: Boolean
        get() = bits.isBitSet(8)
        set(value) {
            bits = bits.setBit(8, value)
        }
    var modifier: Boolean
        get() = bits.isBitSet(9)
        set(value) {
            bits = bits.setBit(9, value)
        }
}

sealed class NinjaModel

/**
 * The model type used in .nj files.
 */
class NjModel(
    /**
     * Sparse list of vertices.
     */
    val vertices: List<NjVertex?>,
    val meshes: List<NjTriangleStrip>,
    val collisionSphereCenter: Vec3,
    val collisionSphereRadius: Float,
) : NinjaModel()

class NjVertex(
    val position: Vec3,
    val normal: Vec3?,
    val boneWeight: Float?,
    val boneWeightStatus: Int,
    val calcContinue: Boolean,
)

class NjTriangleStrip(
    val ignoreLight: Boolean,
    val ignoreSpecular: Boolean,
    val ignoreAmbient: Boolean,
    val useAlpha: Boolean,
    val doubleSide: Boolean,
    val flatShading: Boolean,
    val environmentMapping: Boolean,
    val clockwiseWinding: Boolean,
    val hasTexCoords: Boolean,
    val hasNormal: Boolean,
    var textureId: Int?,
    var srcAlpha: Int?,
    var dstAlpha: Int?,
    val vertices: List<NjMeshVertex>,
)

class NjMeshVertex(
    val index: Int,
    val normal: Vec3?,
    val texCoords: Vec2?,
)

sealed class NjChunk(val typeId: Int) {
    class Unknown(typeId: Int) : NjChunk(typeId)

    object Null : NjChunk(0)

    class BlendAlpha(val srcAlpha: Int, val dstAlpha: Int) : NjChunk(1)

    class MipmapDAdjust(val adjust: Int) : NjChunk(2)

    class SpecularExponent(val specular: Int) : NjChunk(3)

    class CachePolygonList(val cacheIndex: Int) : NjChunk(4)

    class DrawPolygonList(val cacheIndex: Int) : NjChunk(5)

    class Tiny(
        typeId: Int,
        val flipU: Boolean,
        val flipV: Boolean,
        val clampU: Boolean,
        val clampV: Boolean,
        val mipmapDAdjust: UInt,
        val filterMode: Int,
        val superSample: Boolean,
        val textureId: Int,
    ) : NjChunk(typeId)

    class Material(
        typeId: Int,
        val srcAlpha: Int,
        val dstAlpha: Int,
        val diffuse: NjArgb?,
        val ambient: NjArgb?,
        val specular: NjErgb?,
    ) : NjChunk(typeId)

    class Vertex(typeId: Int, val vertices: List<NjChunkVertex>) : NjChunk(typeId)

    class Volume(typeId: Int) : NjChunk(typeId)

    class Strip(typeId: Int, val triangleStrips: List<NjTriangleStrip>) : NjChunk(typeId)

    object End : NjChunk(255)
}

class NjChunkVertex(
    val index: Int,
    val position: Vec3,
    val normal: Vec3?,
    val boneWeight: Float?,
    val boneWeightStatus: Int,
    val calcContinue: Boolean,
)

/**
 * Channels are in range [0, 1].
 */
class NjArgb(
    val a: Float,
    val r: Float,
    val g: Float,
    val b: Float,
)

class NjErgb(
    val e: UByte,
    val r: UByte,
    val g: UByte,
    val b: UByte,
)

/**
 * The model type used in .xj files.
 */
class XjModel(
    val vertices: List<XjVertex>,
    val meshes: List<XjMesh>,
    val collisionSpherePosition: Vec3,
    val collisionSphereRadius: Float,
) : NinjaModel()

class XjVertex(
    val position: Vec3,
    val normal: Vec3?,
    val uv: Vec2?,
)

class XjMesh(
    val material: XjMaterial,
    val indices: List<Int>,
)

class XjMaterial(
    val srcAlpha: Int?,
    val dstAlpha: Int?,
    val textureId: Int?,
    val diffuseR: Int?,
    val diffuseG: Int?,
    val diffuseB: Int?,
    val diffuseA: Int?,
)
