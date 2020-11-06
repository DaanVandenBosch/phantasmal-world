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
    val boneWeight: Float,
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
    var textureId: UInt?,
    var srcAlpha: UByte?,
    var dstAlpha: UByte?,
    val vertices: List<NjMeshVertex>,
)

class NjMeshVertex(
    val index: Int,
    val normal: Vec3?,
    val texCoords: Vec2?,
)

sealed class NjChunk(val typeId: UByte) {
    class Unknown(typeId: UByte) : NjChunk(typeId)

    object Null : NjChunk(0u)

    class Bits(typeId: UByte, val srcAlpha: UByte, val dstAlpha: UByte) : NjChunk(typeId)

    class CachePolygonList(val cacheIndex: UByte, val offset: Int) : NjChunk(4u)

    class DrawPolygonList(val cacheIndex: UByte) : NjChunk(5u)

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
    ) : NjChunk(typeId)

    class Material(
        typeId: UByte,
        val srcAlpha: UByte,
        val dstAlpha: UByte,
        val diffuse: NjArgb?,
        val ambient: NjArgb?,
        val specular: NjErgb?,
    ) : NjChunk(typeId)

    class Vertex(typeId: UByte, val vertices: List<NjChunkVertex>) : NjChunk(typeId)

    class Volume(typeId: UByte) : NjChunk(typeId)

    class Strip(typeId: UByte, val triangleStrips: List<NjTriangleStrip>) : NjChunk(typeId)

    object End : NjChunk(255u)
}

class NjChunkVertex(
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
