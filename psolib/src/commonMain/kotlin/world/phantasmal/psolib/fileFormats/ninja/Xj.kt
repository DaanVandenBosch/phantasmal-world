package world.phantasmal.psolib.fileFormats.ninja

import mu.KotlinLogging
import world.phantasmal.psolib.cursor.Cursor
import world.phantasmal.psolib.fileFormats.Vec2
import world.phantasmal.psolib.fileFormats.Vec3
import world.phantasmal.psolib.fileFormats.vec2Float
import world.phantasmal.psolib.fileFormats.vec3Float

private val logger = KotlinLogging.logger {}

fun parseXjModel(cursor: Cursor): XjModel {
    cursor.seek(4) // Flags according to QEdit, seemingly always 0.
    val vertexInfoTableOffset = cursor.int()
    val vertexInfoCount = cursor.int()
    val triangleStripTableOffset = cursor.int()
    val triangleStripCount = cursor.int()
    val transparentTriangleStripTableOffset = cursor.int()
    val transparentTriangleStripCount = cursor.int()
    val collisionSpherePosition = cursor.vec3Float()
    val collisionSphereRadius = cursor.float()

    val vertices = mutableListOf<XjVertex>()

    if (vertexInfoCount > 0) {
        // TODO: parse all vertex info tables.
        vertices.addAll(parseVertexInfoTable(cursor, vertexInfoTableOffset))
    }

    val meshes = mutableListOf<XjMesh>()

    meshes.addAll(
        parseTriangleStripTable(cursor, triangleStripTableOffset, triangleStripCount),
    )

    meshes.addAll(
        parseTriangleStripTable(
            cursor,
            transparentTriangleStripTableOffset,
            transparentTriangleStripCount,
        ),
    )

    return XjModel(
        vertices,
        meshes,
        collisionSpherePosition,
        collisionSphereRadius,
    )
}

private fun parseVertexInfoTable(cursor: Cursor, vertexInfoTableOffset: Int): List<XjVertex> {
    cursor.seekStart(vertexInfoTableOffset)
    val vertexType = cursor.short().toInt()
    cursor.seek(2) // Flags?
    val vertexTableOffset = cursor.int()
    val vertexSize = cursor.int()
    val vertexCount = cursor.int()

    return (0 until vertexCount).map { i ->
        cursor.seekStart(vertexTableOffset + i * vertexSize)

        val position = cursor.vec3Float()
        var normal: Vec3? = null
        var uv: Vec2? = null

        when (vertexType) {
            2 -> {
                normal = cursor.vec3Float()
            }
            3 -> {
                normal = cursor.vec3Float()
                uv = cursor.vec2Float()
            }
            4 -> {
                // Skip 4 bytes.
            }
            5 -> {
                cursor.seek(4)
                uv = cursor.vec2Float()
            }
            6 -> {
                normal = cursor.vec3Float()
                // Skip 4 bytes.
            }
            7 -> {
                normal = cursor.vec3Float()
                uv = cursor.vec2Float()
            }
            else -> {
                logger.warn { "Unknown vertex type $vertexType with size ${vertexSize}." }
            }
        }

        XjVertex(
            position,
            normal,
            uv,
        )
    }
}

private fun parseTriangleStripTable(
    cursor: Cursor,
    triangleStripListOffset: Int,
    triangleStripCount: Int,
): List<XjMesh> {
    return (0 until triangleStripCount).map { i ->
        cursor.seekStart(triangleStripListOffset + i * 20)

        val materialTableOffset = cursor.int()
        val materialTableSize = cursor.int()
        val indexListOffset = cursor.int()
        val indexCount = cursor.int()

        val material = parseTriangleStripMaterial(
            cursor,
            materialTableOffset,
            materialTableSize,
        )

        cursor.seekStart(indexListOffset)
        val indices = cursor.uShortArray(indexCount)

        XjMesh(
            material,
            indices = indices.map { it.toInt() },
        )
    }
}

private fun parseTriangleStripMaterial(
    cursor: Cursor,
    offset: Int,
    size: Int,
): XjMaterial {
    var srcAlpha: Int? = null
    var dstAlpha: Int? = null
    var textureId: Int? = null
    var diffuseR: Int? = null
    var diffuseG: Int? = null
    var diffuseB: Int? = null
    var diffuseA: Int? = null

    for (i in 0 until size) {
        cursor.seekStart(offset + i * 16)

        when (cursor.int()) {
            2 -> {
                srcAlpha = cursor.int()
                dstAlpha = cursor.int()
            }
            3 -> {
                textureId = cursor.int()
            }
            5 -> {
                diffuseR = cursor.uByte().toInt()
                diffuseG = cursor.uByte().toInt()
                diffuseB = cursor.uByte().toInt()
                diffuseA = cursor.uByte().toInt()
            }
        }
    }

    return XjMaterial(
        srcAlpha,
        dstAlpha,
        textureId,
        diffuseR,
        diffuseG,
        diffuseB,
        diffuseA,
    )
}
