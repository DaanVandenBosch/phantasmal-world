package world.phantasmal.lib.fileFormats.ninja

import mu.KotlinLogging
import world.phantasmal.lib.cursor.Cursor
import world.phantasmal.lib.fileFormats.Vec2
import world.phantasmal.lib.fileFormats.Vec3
import world.phantasmal.lib.fileFormats.vec3F32
import kotlin.math.abs

// TODO:
// - colors
// - bump maps

private val logger = KotlinLogging.logger {}
private const val ZERO_UBYTE: UByte = 0u

class NjcmModel(
    /**
     * Sparse list of vertices.
     */
    val vertices: List<NjcmVertex>,
    val meshes: List<NjcmTriangleStrip>,
    val collisionSphereCenter: Vec3,
    val collisionSphereRadius: Float,
)

class NjcmVertex(
    val position: Vec3,
    val normal: Vec3?,
    val boneWeight: Float,
    val boneWeightStatus: UByte,
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

    class CachePolygonList(val cacheIndex: UByte, val offset: UInt) : NjcmChunk(4u)

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
    val boneWeightStatus: UByte,
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

fun parseNjcmModel(cursor: Cursor, cachedChunkOffsets: MutableMap<UByte, UInt>): NjcmModel {
    val vlistOffset = cursor.u32() // Vertex list
    val plistOffset = cursor.u32() // Triangle strip index list
    val boundingSphereCenter = cursor.vec3F32()
    val boundingSphereRadius = cursor.f32()
    val vertices: MutableList<NjcmVertex> = mutableListOf()
    val meshes: MutableList<NjcmTriangleStrip> = mutableListOf()

    if (vlistOffset != 0u) {
        cursor.seekStart(vlistOffset)

        for (chunk in parseChunks(cursor, cachedChunkOffsets, true)) {
            if (chunk is NjcmChunk.Vertex) {
                for (vertex in chunk.vertices) {
                    vertices[vertex.index] = NjcmVertex(
                        vertex.position,
                        vertex.normal,
                        vertex.boneWeight,
                        vertex.boneWeightStatus,
                        vertex.calcContinue,
                    )
                }
            }
        }
    }

    if (plistOffset != 0u) {
        cursor.seekStart(plistOffset)

        var textureId: UInt? = null
        var srcAlpha: UByte? = null
        var dstAlpha: UByte? = null

        for (chunk in parseChunks(cursor, cachedChunkOffsets, false)) {
            when (chunk) {
                is NjcmChunk.Bits -> {
                    srcAlpha = chunk.srcAlpha
                    dstAlpha = chunk.dstAlpha
                    break
                }

                is NjcmChunk.Tiny -> {
                    textureId = chunk.textureId
                    break
                }

                is NjcmChunk.Material -> {
                    srcAlpha = chunk.srcAlpha
                    dstAlpha = chunk.dstAlpha
                    break
                }

                is NjcmChunk.Strip -> {
                    for (strip in chunk.triangleStrips) {
                        strip.textureId = textureId
                        strip.srcAlpha = srcAlpha
                        strip.dstAlpha = dstAlpha
                    }

                    meshes.addAll(chunk.triangleStrips)
                    break
                }

                else -> {
                    // Ignore
                }
            }
        }
    }

    return NjcmModel(
        vertices,
        meshes,
        boundingSphereCenter,
        boundingSphereRadius,
    )
}

// TODO: don't reparse when DrawPolygonList chunk is encountered.
private fun parseChunks(
    cursor: Cursor,
    cachedChunkOffsets: MutableMap<UByte, UInt>,
    wideEndChunks: Boolean,
): List<NjcmChunk> {
    val chunks: MutableList<NjcmChunk> = mutableListOf()
    var loop = true

    while (loop) {
        val typeId = cursor.u8()
        val flags = cursor.u8()
        val flagsUInt = flags.toUInt()
        val chunkStartPosition = cursor.position
        var size = 0u

        when (typeId.toInt()) {
            0 -> {
                chunks.add(NjcmChunk.Null)
            }
            in 1..3 -> {
                chunks.add(NjcmChunk.Bits(
                    typeId,
                    srcAlpha = ((flagsUInt shr 3).toUByte() and 0b111u),
                    dstAlpha = flags and 0b111u,
                ))
            }
            4 -> {
                val cacheIndex = flags
                val offset = cursor.position

                chunks.add(NjcmChunk.CachePolygonList(
                    cacheIndex,
                    offset,
                ))

                cachedChunkOffsets[cacheIndex] = offset
                loop = false
            }
            5 -> {
                val cacheIndex = flags
                val cachedOffset = cachedChunkOffsets[cacheIndex]

                if (cachedOffset != null) {
                    cursor.seekStart(cachedOffset)
                    chunks.addAll(parseChunks(cursor, cachedChunkOffsets, wideEndChunks))
                }

                chunks.add(NjcmChunk.DrawPolygonList(
                    cacheIndex,
                ))
            }
            in 8..9 -> {
                size = 2u
                val textureBitsAndId = cursor.u16().toUInt()

                chunks.add(NjcmChunk.Tiny(
                    typeId,
                    flipU = (typeId.toUInt() and 0x80u) != 0u,
                    flipV = (typeId.toUInt() and 0x40u) != 0u,
                    clampU = (typeId.toUInt() and 0x20u) != 0u,
                    clampV = (typeId.toUInt() and 0x10u) != 0u,
                    mipmapDAdjust = typeId.toUInt() and 0b1111u,
                    filterMode = textureBitsAndId shr 14,
                    superSample = (textureBitsAndId and 0x40u) != 0u,
                    textureId = textureBitsAndId and 0x1fffu,
                ))
            }
            in 17..31 -> {
                size = 2u + 2u * cursor.u16()

                var diffuse: NjcmArgb? = null
                var ambient: NjcmArgb? = null
                var specular: NjcmErgb? = null

                if ((flagsUInt and 0b1u) != 0u) {
                    diffuse = NjcmArgb(
                        b = cursor.u8().toFloat() / 255f,
                        g = cursor.u8().toFloat() / 255f,
                        r = cursor.u8().toFloat() / 255f,
                        a = cursor.u8().toFloat() / 255f,
                    )
                }

                if ((flagsUInt and 0b10u) != 0u) {
                    ambient = NjcmArgb(
                        b = cursor.u8().toFloat() / 255f,
                        g = cursor.u8().toFloat() / 255f,
                        r = cursor.u8().toFloat() / 255f,
                        a = cursor.u8().toFloat() / 255f,
                    )
                }

                if ((flagsUInt and 0b100u) != 0u) {
                    specular = NjcmErgb(
                        b = cursor.u8(),
                        g = cursor.u8(),
                        r = cursor.u8(),
                        e = cursor.u8(),
                    )
                }

                chunks.add(NjcmChunk.Material(
                    typeId,
                    srcAlpha = ((flagsUInt shr 3).toUByte() and 0b111u),
                    dstAlpha = flags and 0b111u,
                    diffuse,
                    ambient,
                    specular,
                ))
            }
            in 32..50 -> {
                size = 2u + 4u * cursor.u16()
                chunks.add(NjcmChunk.Vertex(
                    typeId,
                    vertices = parseVertexChunk(cursor, typeId, flags),
                ))
            }
            in 56..58 -> {
                size = 2u + 2u * cursor.u16()
                chunks.add(NjcmChunk.Volume(
                    typeId,
                ))
            }
            in 64..75 -> {
                size = 2u + 2u * cursor.u16()
                chunks.add(NjcmChunk.Strip(
                    typeId,
                    triangleStrips = parseTriangleStripChunk(cursor, typeId, flags),
                ))
            }
            255 -> {
                size = if (wideEndChunks) 2u else 0u
                chunks.add(NjcmChunk.End)
                loop = false
            }
            else -> {
                size = 2u + 2u * cursor.u16()
                chunks.add(NjcmChunk.Unknown(
                    typeId,
                ))
                logger.warn { "Unknown chunk type $typeId at offset ${chunkStartPosition}." }
            }
        }

        cursor.seekStart(chunkStartPosition + size)
    }

    return chunks
}

private fun parseVertexChunk(
    cursor: Cursor,
    chunkTypeId: UByte,
    flags: UByte,
): List<NjcmChunkVertex> {
    val boneWeightStatus = flags and 0b11u
    val calcContinue = (flags and 0x80u) != ZERO_UBYTE

    val index = cursor.u16()
    val vertexCount = cursor.u16()

    val vertices: MutableList<NjcmChunkVertex> = mutableListOf()

    for (i in (0u).toUShort() until vertexCount) {
        var vertexIndex = index + i
        val position = cursor.vec3F32()
        var normal: Vec3? = null
        var boneWeight = 1f

        when (chunkTypeId.toInt()) {
            32 -> {
                // NJDCVSH
                cursor.seek(4) // Always 1.0
            }
            33 -> {
                // NJDCVVNSH
                cursor.seek(4) // Always 1.0
                normal = cursor.vec3F32()
                cursor.seek(4) // Always 0.0
            }
            in 35..40 -> {
                if (chunkTypeId == (37u).toUByte()) {
                    // NJDCVNF
                    // NinjaFlags32
                    vertexIndex = index + cursor.u16()
                    boneWeight = cursor.u16().toFloat() / 255f
                } else {
                    // Skip user flags and material information.
                    cursor.seek(4)
                }
            }
            41 -> {
                normal = cursor.vec3F32()
            }
            in 42..47 -> {
                normal = cursor.vec3F32()

                if (chunkTypeId == (44u).toUByte()) {
                    // NJDCVVNNF
                    // NinjaFlags32
                    vertexIndex = index + cursor.u16()
                    boneWeight = cursor.u16().toFloat() / 255f
                } else {
                    // Skip user flags and material information.
                    cursor.seek(4)
                }
            }
            in 48..50 -> {
                // 32-Bit vertex normal in format: reserved(2)|x(10)|y(10)|z(10)
                val n = cursor.u32()
                normal = Vec3(
                    ((n shr 20) and 0x3ffu).toFloat() / 0x3ff,
                    ((n shr 10) and 0x3ffu).toFloat() / 0x3ff,
                    (n and 0x3ffu).toFloat() / 0x3ff,
                )

                if (chunkTypeId >= 49u) {
                    // Skip user flags and material information.
                    cursor.seek(4)
                }
            }
        }

        vertices.add(NjcmChunkVertex(
            vertexIndex.toInt(),
            position,
            normal,
            boneWeight,
            boneWeightStatus,
            calcContinue,
        ))
    }

    return vertices
}

private fun parseTriangleStripChunk(
    cursor: Cursor,
    chunkTypeId: UByte,
    flags: UByte,
): List<NjcmTriangleStrip> {
    val ignoreLight = (flags and 0b1u) != ZERO_UBYTE
    val ignoreSpecular = (flags and 0b10u) != ZERO_UBYTE
    val ignoreAmbient = (flags and 0b100u) != ZERO_UBYTE
    val useAlpha = (flags and 0b1000u) != ZERO_UBYTE
    val doubleSide = (flags and 0b10000u) != ZERO_UBYTE
    val flatShading = (flags and 0b100000u) != ZERO_UBYTE
    val environmentMapping = (flags and 0b1000000u) != ZERO_UBYTE

    val userOffsetAndStripCount = cursor.u16()
    val userFlagsSize = (userOffsetAndStripCount.toUInt() shr 14).toInt()
    val stripCount = userOffsetAndStripCount and 0x3fffu

    var hasTexCoords = false
    var hasColor = false
    var hasNormal = false
    var hasDoubleTexCoords = false

    when (chunkTypeId.toInt()) {
        64 -> {
        }
        65, 66 -> {
            hasTexCoords = true
        }
        67 -> {
            hasNormal = true
        }
        68, 69 -> {
            hasTexCoords = true
            hasNormal = true
        }
        70 -> {
            hasColor = true
        }
        71, 72 -> {
            hasTexCoords = true
            hasColor = true
        }
        73 -> {
        }
        74, 75 -> {
            hasDoubleTexCoords = true
        }
        else -> error("Unexpected chunk type ID: ${chunkTypeId}.")
    }

    val strips: MutableList<NjcmTriangleStrip> = mutableListOf()

    repeat(stripCount.toInt()) {
        val windingFlagAndIndexCount = cursor.i16()
        val clockwiseWinding = windingFlagAndIndexCount < 1
        val indexCount = abs(windingFlagAndIndexCount.toInt())

        val vertices: MutableList<NjcmMeshVertex> = mutableListOf()

        for (j in 0..indexCount) {
            val index = cursor.u16()

            val texCoords = if (hasTexCoords) {
                Vec2(cursor.u16().toFloat() / 255f, cursor.u16().toFloat() / 255f)
            } else null

            // Ignore ARGB8888 color.
            if (hasColor) {
                cursor.seek(4)
            }

            val normal = if (hasNormal) {
                Vec3(
                    cursor.u16().toFloat() / 255f,
                    cursor.u16().toFloat() / 255f,
                    cursor.u16().toFloat() / 255f,
                )
            } else null

            // Ignore double texture coordinates (Ua, Vb, Ua, Vb).
            if (hasDoubleTexCoords) {
                cursor.seek(8)
            }

            // User flags start at the third vertex because they are per-triangle.
            if (j >= 2) {
                cursor.seek(2 * userFlagsSize)
            }

            vertices.add(NjcmMeshVertex(
                index,
                normal,
                texCoords,
            ))
        }

        strips.add(NjcmTriangleStrip(
            ignoreLight,
            ignoreSpecular,
            ignoreAmbient,
            useAlpha,
            doubleSide,
            flatShading,
            environmentMapping,
            clockwiseWinding,
            hasTexCoords,
            hasNormal,
            textureId = null,
            srcAlpha = null,
            dstAlpha = null,
            vertices,
        ))
    }

    return strips
}
