package world.phantasmal.lib.fileFormats

import world.phantasmal.core.isBitSet
import world.phantasmal.lib.cursor.Cursor
import world.phantasmal.lib.fileFormats.ninja.NinjaObject
import world.phantasmal.lib.fileFormats.ninja.XjModel
import world.phantasmal.lib.fileFormats.ninja.angleToRad
import world.phantasmal.lib.fileFormats.ninja.parseXjObject

class RenderObject(
    val sections: List<RenderSection>,
)

class RenderSection(
    val id: Int,
    val position: Vec3,
    val rotation: Vec3,
    val objects: List<NinjaObject<XjModel>>,
)

fun parseAreaGeometry(cursor: Cursor): RenderObject {
    val sections = mutableListOf<RenderSection>()

    cursor.seekEnd(16)

    val dataOffset = parseRel(cursor, parseIndex = false).dataOffset
    cursor.seekStart(dataOffset)
    cursor.seek(8) // Format "fmt2" in UTF-16.
    val sectionCount = cursor.int()
    cursor.seek(4)
    val sectionTableOffset = cursor.int()
    // val textureNameOffset = cursor.int()

    for (i in 0 until sectionCount) {
        cursor.seekStart(sectionTableOffset + 52 * i)

        val sectionId = cursor.int()
        val sectionPosition = cursor.vec3Float()
        val sectionRotation = Vec3(
            angleToRad(cursor.int()),
            angleToRad(cursor.int()),
            angleToRad(cursor.int()),
        )

        cursor.seek(4)

        val simpleGeometryOffsetTableOffset = cursor.int()
//         val animatedGeometryOffsetTableOffset = cursor.int()
        cursor.seek(4)
        val simpleGeometryOffsetCount = cursor.int()
//         val animatedGeometryOffsetCount = cursor.int()
        // Ignore animatedGeometryOffsetCount and the last 4 bytes.

        val objects = parseGeometryTable(
            cursor,
            simpleGeometryOffsetTableOffset,
            simpleGeometryOffsetCount,
        )

        sections.add(RenderSection(
            sectionId,
            sectionPosition,
            sectionRotation,
            objects,
        ))
    }

    return RenderObject(sections)
}

// TODO: don't reparse the same objects multiple times. Create DAG instead of tree.
private fun parseGeometryTable(
    cursor: Cursor,
    tableOffset: Int,
    tableEntryCount: Int,
): List<NinjaObject<XjModel>> {
    val objects = mutableListOf<NinjaObject<XjModel>>()

    for (i in 0 until tableEntryCount) {
        cursor.seekStart(tableOffset + 16 * i)

        var offset = cursor.int()
        cursor.seek(8)
        val flags = cursor.int()

        if (flags.isBitSet(2)) {
            offset = cursor.seekStart(offset).int()
        }

        cursor.seekStart(offset)
        objects.addAll(parseXjObject(cursor))
    }

    return objects
}
