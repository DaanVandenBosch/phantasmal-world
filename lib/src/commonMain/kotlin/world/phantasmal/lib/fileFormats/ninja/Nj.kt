package world.phantasmal.lib.fileFormats.ninja

import mu.KotlinLogging
import world.phantasmal.core.isBitSet
import world.phantasmal.lib.cursor.Cursor
import world.phantasmal.lib.fileFormats.Vec2
import world.phantasmal.lib.fileFormats.Vec3
import world.phantasmal.lib.fileFormats.vec3Float
import kotlin.math.abs

// TODO:
//  - colors
//  - bump maps

private val logger = KotlinLogging.logger {}

// TODO: Simplify parser by not parsing chunks into vertices and meshes. Do the chunk to vertex/mesh
//       conversion at a higher level.
fun parseNjModel(cursor: Cursor, cachedChunkOffsets: MutableMap<Int, Int>): NjModel {
    val vlistOffset = cursor.int() // Vertex list
    val plistOffset = cursor.int() // Triangle strip index list
    val collisionSphereCenter = cursor.vec3Float()
    val collisionSphereRadius = cursor.float()
    val vertices: MutableList<NjVertex?> = mutableListOf()
    val meshes: MutableList<NjTriangleStrip> = mutableListOf()

    if (vlistOffset != 0) {
        cursor.seekStart(vlistOffset)

        for (chunk in parseChunks(cursor, cachedChunkOffsets, true)) {
            if (chunk is NjChunk.Vertex) {
                for (vertex in chunk.vertices) {
                    while (vertices.size <= vertex.index) {
                        vertices.add(null)
                    }

                    vertices[vertex.index] = NjVertex(
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

    if (plistOffset != 0) {
        cursor.seekStart(plistOffset)

        var textureId: Int? = null
        var srcAlpha: Int? = null
        var dstAlpha: Int? = null

        for (chunk in parseChunks(cursor, cachedChunkOffsets, false)) {
            when (chunk) {
                is NjChunk.Bits -> {
                    srcAlpha = chunk.srcAlpha
                    dstAlpha = chunk.dstAlpha
                }

                is NjChunk.Tiny -> {
                    textureId = chunk.textureId
                }

                is NjChunk.Material -> {
                    srcAlpha = chunk.srcAlpha
                    dstAlpha = chunk.dstAlpha
                }

                is NjChunk.Strip -> {
                    for (strip in chunk.triangleStrips) {
                        strip.textureId = textureId
                        strip.srcAlpha = srcAlpha
                        strip.dstAlpha = dstAlpha
                    }

                    meshes.addAll(chunk.triangleStrips)
                }

                else -> {
                    // Ignore
                }
            }
        }
    }

    return NjModel(
        vertices,
        meshes,
        collisionSphereCenter,
        collisionSphereRadius,
    )
}

// TODO: don't reparse when DrawPolygonList chunk is encountered.
private fun parseChunks(
    cursor: Cursor,
    cachedChunkOffsets: MutableMap<Int, Int>,
    wideEndChunks: Boolean,
): List<NjChunk> {
    val chunks: MutableList<NjChunk> = mutableListOf()
    var loop = true

    while (loop) {
        val typeId = cursor.uByte()
        val flags = cursor.uByte().toInt()
        val chunkStartPosition = cursor.position
        var size = 0

        when (typeId.toInt()) {
            0 -> {
                chunks.add(NjChunk.Null)
            }
            in 1..3 -> {
                chunks.add(NjChunk.Bits(
                    typeId,
                    srcAlpha = (flags ushr 3) and 0b111,
                    dstAlpha = flags and 0b111,
                ))
            }
            4 -> {
                val offset = cursor.position

                chunks.add(NjChunk.CachePolygonList(
                    cacheIndex = flags,
                    offset,
                ))

                cachedChunkOffsets[flags] = offset
                loop = false
            }
            5 -> {
                val cachedOffset = cachedChunkOffsets[flags]

                if (cachedOffset != null) {
                    cursor.seekStart(cachedOffset)
                    chunks.addAll(parseChunks(cursor, cachedChunkOffsets, wideEndChunks))
                }

                chunks.add(NjChunk.DrawPolygonList(
                    cacheIndex = flags,
                ))
            }
            in 8..9 -> {
                size = 2
                val textureBitsAndId = cursor.uShort().toInt()

                chunks.add(NjChunk.Tiny(
                    typeId,
                    flipU = typeId.isBitSet(7),
                    flipV = typeId.isBitSet(6),
                    clampU = typeId.isBitSet(5),
                    clampV = typeId.isBitSet(4),
                    mipmapDAdjust = typeId.toUInt() and 0b1111u,
                    filterMode = textureBitsAndId ushr 14,
                    superSample = (textureBitsAndId and 0x40) != 0,
                    textureId = textureBitsAndId and 0x1fff,
                ))
            }
            in 17..31 -> {
                size = 2 + 2 * cursor.short()

                var diffuse: NjArgb? = null
                var ambient: NjArgb? = null
                var specular: NjErgb? = null

                if (flags.isBitSet(0)) {
                    diffuse = NjArgb(
                        b = cursor.uByte().toFloat() / 255f,
                        g = cursor.uByte().toFloat() / 255f,
                        r = cursor.uByte().toFloat() / 255f,
                        a = cursor.uByte().toFloat() / 255f,
                    )
                }

                if (flags.isBitSet(1)) {
                    ambient = NjArgb(
                        b = cursor.uByte().toFloat() / 255f,
                        g = cursor.uByte().toFloat() / 255f,
                        r = cursor.uByte().toFloat() / 255f,
                        a = cursor.uByte().toFloat() / 255f,
                    )
                }

                if (flags.isBitSet(2)) {
                    specular = NjErgb(
                        b = cursor.uByte(),
                        g = cursor.uByte(),
                        r = cursor.uByte(),
                        e = cursor.uByte(),
                    )
                }

                chunks.add(NjChunk.Material(
                    typeId,
                    srcAlpha = (flags ushr 3) and 0b111,
                    dstAlpha = flags and 0b111,
                    diffuse,
                    ambient,
                    specular,
                ))
            }
            in 32..50 -> {
                size = 2 + 4 * cursor.short()
                chunks.add(NjChunk.Vertex(
                    typeId,
                    vertices = parseVertexChunk(cursor, typeId, flags),
                ))
            }
            in 56..58 -> {
                size = 2 + 2 * cursor.short()
                chunks.add(NjChunk.Volume(
                    typeId,
                ))
            }
            in 64..75 -> {
                size = 2 + 2 * cursor.short()
                chunks.add(NjChunk.Strip(
                    typeId,
                    triangleStrips = parseTriangleStripChunk(cursor, typeId, flags),
                ))
            }
            255 -> {
                size = if (wideEndChunks) 2 else 0
                chunks.add(NjChunk.End)
                loop = false
            }
            else -> {
                size = 2 + 2 * cursor.short()
                chunks.add(NjChunk.Unknown(
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
    flags: Int,
): List<NjChunkVertex> {
    val boneWeightStatus = flags and 0b11
    val calcContinue = (flags and 0x80) != 0

    val index = cursor.uShort()
    val vertexCount = cursor.uShort()

    val vertices: MutableList<NjChunkVertex> = mutableListOf()

    for (i in (0u).toUShort() until vertexCount) {
        var vertexIndex = index + i
        val position = cursor.vec3Float()
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
                normal = cursor.vec3Float()
                cursor.seek(4) // Always 0.0
            }
            in 35..40 -> {
                if (chunkTypeId == (37u).toUByte()) {
                    // NJDCVNF
                    // NinjaFlags32
                    vertexIndex = index + cursor.uShort()
                    boneWeight = cursor.uShort().toFloat() / 255f
                } else {
                    // Skip user flags and material information.
                    cursor.seek(4)
                }
            }
            41 -> {
                normal = cursor.vec3Float()
            }
            in 42..47 -> {
                normal = cursor.vec3Float()

                if (chunkTypeId == (44u).toUByte()) {
                    // NJDCVVNNF
                    // NinjaFlags32
                    vertexIndex = index + cursor.uShort()
                    boneWeight = cursor.uShort().toFloat() / 255f
                } else {
                    // Skip user flags and material information.
                    cursor.seek(4)
                }
            }
            in 48..50 -> {
                // 32-Bit vertex normal in format: reserved(2)|x(10)|y(10)|z(10)
                val n = cursor.uInt()
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

        vertices.add(NjChunkVertex(
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
    flags: Int,
): List<NjTriangleStrip> {
    val ignoreLight = flags.isBitSet(0)
    val ignoreSpecular = flags.isBitSet(1)
    val ignoreAmbient = flags.isBitSet(2)
    val useAlpha = flags.isBitSet(3)
    val doubleSide = flags.isBitSet(4)
    val flatShading = flags.isBitSet(5)
    val environmentMapping = flags.isBitSet(6)

    val userOffsetAndStripCount = cursor.short().toInt()
    val userFlagsSize = (userOffsetAndStripCount ushr 14)
    val stripCount = userOffsetAndStripCount and 0x3FFF

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

    val strips: MutableList<NjTriangleStrip> = mutableListOf()

    repeat(stripCount) {
        val windingFlagAndIndexCount = cursor.short()
        val clockwiseWinding = windingFlagAndIndexCount < 1
        val indexCount = abs(windingFlagAndIndexCount.toInt())

        val vertices: MutableList<NjMeshVertex> = mutableListOf()

        for (j in 0 until indexCount) {
            val index = cursor.uShort().toInt()

            val texCoords = if (hasTexCoords) {
                Vec2(cursor.uShort().toFloat() / 255f, cursor.uShort().toFloat() / 255f)
            } else null

            // Ignore ARGB8888 color.
            if (hasColor) {
                cursor.seek(4)
            }

            val normal = if (hasNormal) {
                Vec3(
                    cursor.uShort().toFloat() / 255f,
                    cursor.uShort().toFloat() / 255f,
                    cursor.uShort().toFloat() / 255f,
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

            vertices.add(NjMeshVertex(
                index,
                normal,
                texCoords,
            ))
        }

        strips.add(NjTriangleStrip(
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
