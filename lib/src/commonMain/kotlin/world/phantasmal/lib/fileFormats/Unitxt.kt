package world.phantasmal.lib.fileFormats

import world.phantasmal.lib.cursor.Cursor
import kotlin.math.min

class Unitxt(val categories: List<List<String>>)

fun parseUnitxt(cursor: Cursor): Unitxt {
    val categoryCount = cursor.int()
    val entryCounts = cursor.intArray(categoryCount)

    val categoryEntryOffsets: List<IntArray> = entryCounts.map { entryCount ->
        cursor.intArray(entryCount)
    }

    val categories = categoryEntryOffsets.map { entryOffsets ->
        entryOffsets.map { entryOffset ->
            cursor.seekStart(entryOffset)
            cursor.stringUtf16(
                min(1024, cursor.bytesLeft),
                nullTerminated = true,
                dropRemaining = true,
            )
        }
    }

    return Unitxt(categories)
}
