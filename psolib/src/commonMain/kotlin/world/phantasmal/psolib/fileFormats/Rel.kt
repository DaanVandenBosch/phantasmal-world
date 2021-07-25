package world.phantasmal.psolib.fileFormats

import world.phantasmal.psolib.cursor.Cursor

class Rel(
    /**
     * Offset from which to start parsing the file.
     */
    val dataOffset: Int,
    /**
     * List of offsets into the file, presumably used by Sega to fix pointers after loading a file
     * directly into memory.
     */
    val index: List<RelIndexEntry>,
)

class RelIndexEntry(
    val offset: Int,
    val size: Int,
)

fun parseRel(cursor: Cursor, parseIndex: Boolean): Rel {
    cursor.seekEnd(32)

    val indexOffset = cursor.int()
    val indexSize = cursor.int()
    cursor.seek(8) // Typically 1, 0, 0,...
    val dataOffset = cursor.int()
    // Typically followed by 12 nul bytes.

    cursor.seekStart(indexOffset)
    val index = if (parseIndex) parseIndices(cursor, indexSize) else emptyList()

    return Rel(dataOffset, index)
}

private fun parseIndices(cursor: Cursor, indexSize: Int): List<RelIndexEntry> {
    val compactOffsets = cursor.uShortArray(indexSize)
    var expandedOffset = 0

    return compactOffsets.map { compactOffset ->
        expandedOffset += 4 * compactOffset.toInt()

        // Size is not always present.
        cursor.seekStart(expandedOffset - 4)
        val size = cursor.int()
        val offset = cursor.int()
        RelIndexEntry(offset, size)
    }
}
