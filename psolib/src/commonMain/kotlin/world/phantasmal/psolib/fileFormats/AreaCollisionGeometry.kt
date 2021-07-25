package world.phantasmal.psolib.fileFormats

import world.phantasmal.psolib.cursor.Cursor

class CollisionGeometry(
    val meshes: List<CollisionMesh>,
)

class CollisionMesh(
    val vertices: List<Vec3>,
    val triangles: List<CollisionTriangle>,
)

class CollisionTriangle(
    val index1: Int,
    val index2: Int,
    val index3: Int,
    val flags: Int,
    val normal: Vec3,
)

fun parseAreaCollisionGeometry(cursor: Cursor): CollisionGeometry {
    val dataOffset = parseRel(cursor, parseIndex = false).dataOffset
    cursor.seekStart(dataOffset)
    val mainOffsetTableOffset = cursor.int()
    cursor.seekStart(mainOffsetTableOffset)

    val meshes = mutableListOf<CollisionMesh>()

    while (cursor.hasBytesLeft()) {
        val startPos = cursor.position
        val blockTrailerOffset = cursor.int()

        if (blockTrailerOffset == 0) {
            break
        }

        val vertices = mutableListOf<Vec3>()
        val triangles = mutableListOf<CollisionTriangle>()
        meshes.add(CollisionMesh(vertices, triangles))

        cursor.seekStart(blockTrailerOffset)

        val vertexCount = cursor.int()
        val vertexTableOffset = cursor.int()
        val triangleCount = cursor.int()
        val triangleTableOffset = cursor.int()

        cursor.seekStart(vertexTableOffset)

        repeat(vertexCount) {
            vertices.add(cursor.vec3Float())
        }

        cursor.seekStart(triangleTableOffset)

        repeat(triangleCount) {
            val index1 = cursor.uShort().toInt()
            val index2 = cursor.uShort().toInt()
            val index3 = cursor.uShort().toInt()
            val flags = cursor.uShort().toInt()
            val normal = cursor.vec3Float()
            cursor.seek(16)

            triangles.add(CollisionTriangle(
                index1,
                index2,
                index3,
                flags,
                normal
            ))
        }

        cursor.seekStart(startPos + 24)
    }

    return CollisionGeometry(meshes)
}
