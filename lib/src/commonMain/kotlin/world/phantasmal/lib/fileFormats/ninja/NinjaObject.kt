package world.phantasmal.lib.fileFormats.ninja

import world.phantasmal.lib.fileFormats.Vec2
import world.phantasmal.lib.fileFormats.Vec3

class NinjaObject<Model : NinjaModel>(
    val evaluationFlags: NinjaEvaluationFlags,
    val model: Model?,
    val position: Vec3,
    /**
     * Euler angles in radians.
     */
    val rotation: Vec3,
    val scale: Vec3,
    children: MutableList<NinjaObject<Model>>,
) {
    private val _children = children
    val children: List<NinjaObject<Model>> = _children

    fun addChild(child: NinjaObject<Model>) {
        _children.add(child)
    }
}

class NinjaEvaluationFlags(
    var noTranslate: Boolean,
    var noRotate: Boolean,
    var noScale: Boolean,
    var hidden: Boolean,
    var breakChildTrace: Boolean,
    var zxyRotationOrder: Boolean,
    var skip: Boolean,
    var shapeSkip: Boolean,
)

sealed class NinjaModel

/**
 * The model type used in .nj files.
 */
class NjcmModel(
    /**
     * Sparse list of vertices.
     */
    val vertices: List<NjcmVertex?>,
    val meshes: List<NjcmTriangleStrip>,
    val collisionSphereCenter: Vec3,
    val collisionSphereRadius: Float,
) : NinjaModel()

class NjcmVertex(
    val position: Vec3,
    val normal: Vec3?,
    val boneWeight: Float,
    val boneWeightStatus: Int,
    val calcContinue: Boolean,
)

class NjcmTriangleStrip(
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
    var textureId: UInt?,
    var srcAlpha: UByte?,
    var dstAlpha: UByte?,
    val vertices: List<NjcmMeshVertex>,
)

class NjcmMeshVertex(
    val index: UShort,
    val normal: Vec3?,
    val texCoords: Vec2?,
)

sealed class NjcmChunk(val typeId: UByte) {
    class Unknown(typeId: UByte) : NjcmChunk(typeId)

    object Null : NjcmChunk(0u)

    class Bits(typeId: UByte, val srcAlpha: UByte, val dstAlpha: UByte) : NjcmChunk(typeId)

    class CachePolygonList(val cacheIndex: UByte, val offset: Int) : NjcmChunk(4u)

    class DrawPolygonList(val cacheIndex: UByte) : NjcmChunk(5u)

    class Tiny(
        typeId: UByte,
        val flipU: Boolean,
        val flipV: Boolean,
        val clampU: Boolean,
        val clampV: Boolean,
        val mipmapDAdjust: UInt,
        val filterMode: UInt,
        val superSample: Boolean,
        val textureId: UInt,
    ) : NjcmChunk(typeId)

    class Material(
        typeId: UByte,
        val srcAlpha: UByte,
        val dstAlpha: UByte,
        val diffuse: NjcmArgb?,
        val ambient: NjcmArgb?,
        val specular: NjcmErgb?,
    ) : NjcmChunk(typeId)

    class Vertex(typeId: UByte, val vertices: List<NjcmChunkVertex>) : NjcmChunk(typeId)

    class Volume(typeId: UByte) : NjcmChunk(typeId)

    class Strip(typeId: UByte, val triangleStrips: List<NjcmTriangleStrip>) : NjcmChunk(typeId)

    object End : NjcmChunk(255u)
}

class NjcmChunkVertex(
    val index: Int,
    val position: Vec3,
    val normal: Vec3?,
    val boneWeight: Float,
    val boneWeightStatus: Int,
    val calcContinue: Boolean,
)

/**
 * Channels are in range [0, 1].
 */
class NjcmArgb(
    val a: Float,
    val r: Float,
    val g: Float,
    val b: Float,
)

class NjcmErgb(
    val e: UByte,
    val r: UByte,
    val g: UByte,
    val b: UByte,
)

/**
 * The model type used in .xj files.
 */
class XjModel : NinjaModel()
