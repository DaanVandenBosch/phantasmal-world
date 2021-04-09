package world.phantasmal.lib.fileFormats

import world.phantasmal.core.isBitSet
import world.phantasmal.lib.cursor.Cursor
import world.phantasmal.lib.fileFormats.ninja.XjObject
import world.phantasmal.lib.fileFormats.ninja.angleToRad
import world.phantasmal.lib.fileFormats.ninja.parseXjObject

class RenderGeometry(
    val sections: List<RenderSection>,
)

class RenderSection(
    val id: Int,
    val position: Vec3,
    val rotation: Vec3,
    val objects: List<XjObject>,
    val animatedObjects: List<XjObject>,
)

fun parseAreaRenderGeometry(cursor: Cursor): RenderGeometry {
    val sections = mutableListOf<RenderSection>()

    cursor.seekEnd(16)

    val dataOffset = parseRel(cursor, parseIndex = false).dataOffset
    cursor.seekStart(dataOffset)
    cursor.seek(8) // Format "fmt2" in UTF-16.
    val sectionCount = cursor.int()
    cursor.seek(4)
    val sectionTableOffset = cursor.int()
    // val textureNameOffset = cursor.int()

    val xjObjectCache = mutableMapOf<Int, List<XjObject>>()

    for (i in 0 until sectionCount) {
        cursor.seekStart(sectionTableOffset + 52 * i)

        val sectionId = cursor.int()
        val sectionPosition = cursor.vec3Float()
        val sectionRotation = Vec3(
            angleToRad(cursor.int()),
            angleToRad(cursor.int()),
            angleToRad(cursor.int()),
        )

        cursor.seek(4) // Radius?

        val simpleGeometryOffsetTableOffset = cursor.int()
        val animatedGeometryOffsetTableOffset = cursor.int()
        val simpleGeometryOffsetCount = cursor.int()
        val animatedGeometryOffsetCount = cursor.int()
        // Ignore the last 4 bytes.

        val objects = parseGeometryTable(
            cursor,
            xjObjectCache,
            simpleGeometryOffsetTableOffset,
            simpleGeometryOffsetCount,
            animated = false,
        )

        val animatedObjects = parseGeometryTable(
            cursor,
            xjObjectCache,
            animatedGeometryOffsetTableOffset,
            animatedGeometryOffsetCount,
            animated = true,
        )

        sections.add(RenderSection(
            sectionId,
            sectionPosition,
            sectionRotation,
            objects,
            animatedObjects,
        ))
    }

    return RenderGeometry(sections)
}

private fun parseGeometryTable(
    cursor: Cursor,
    xjObjectCache: MutableMap<Int, List<XjObject>>,
    tableOffset: Int,
    tableEntryCount: Int,
    animated: Boolean,
): List<XjObject> {
    val tableEntrySize = if (animated) 32 else 16
    val objects = mutableListOf<XjObject>()

    for (i in 0 until tableEntryCount) {
        cursor.seekStart(tableOffset + tableEntrySize * i)

        var offset = cursor.int()
        cursor.seek(8)
        val flags = cursor.int()

        if (flags.isBitSet(2)) {
            offset = cursor.seekStart(offset).int()
        }

        objects.addAll(
            xjObjectCache.getOrPut(offset) {
                cursor.seekStart(offset)
                parseXjObject(cursor)
            }
        )
    }

    return objects
}
