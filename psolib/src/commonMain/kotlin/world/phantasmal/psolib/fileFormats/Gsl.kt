package world.phantasmal.psolib.fileFormats

import mu.KotlinLogging
import world.phantasmal.core.PwResult
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.Cursor

private val logger = KotlinLogging.logger {}

private const val ENTRY_SIZE = 0x30
private const val NAME_SIZE = 0x20
private const val PAGE_SIZE = 0x800

class GslEntry(val name: String, val data: Buffer)

/**
 * An index entry for a file inside a GSL archive: byte offset and size.
 */
class GslIndexEntry(val name: String, val offset: Int, val size: Int)

/**
 * GSL is a simple archive format used by PSO for e.g. item parameter tables.
 *
 * Format:
 * - Header entries, each 0x30 bytes: 0x20 bytes ASCII filename, 4 bytes offset (in pages of
 *   0x800), 4 bytes size, 8 bytes unused.
 * - A filename starting with a zero byte marks the end of the header.
 */

/**
 * Parse only the GSL header/index, without reading file data.
 * Useful for building a lookup table when data will be read on demand (e.g. via File.slice).
 *
 * @param fileSize total file size for bounds checking; if null, no bounds check is performed.
 */
fun parseGslIndex(cursor: Cursor, fileSize: Long? = null): List<GslIndexEntry> {
    val entries = mutableListOf<GslIndexEntry>()

    while (cursor.bytesLeft >= ENTRY_SIZE) {
        val firstByte = cursor.uByte()
        if (firstByte == 0.toUByte()) break

        cursor.seek(-1)
        val name = cursor.stringAscii(NAME_SIZE, nullTerminated = true, dropRemaining = true)
        // Read offset as unsigned to avoid negative values for entries beyond 2 GB.
        val offsetPages = cursor.uInt().toLong()
        val size = cursor.int()
        cursor.seek(8) // skip unused bytes

        if (size < 0) continue // Corrupt entry, skip.

        val offset = offsetPages * PAGE_SIZE

        if (offset > Int.MAX_VALUE) continue // Offset exceeds Int range, skip entry.

        if (fileSize == null || offset + size <= fileSize) {
            entries.add(GslIndexEntry(name, offset.toInt(), size))
        }
    }

    return entries
}

/**
 * Parses a GSL archive, reading all file data into memory.
 */
fun parseGsl(cursor: Cursor): PwResult<List<GslEntry>> {
    val result = PwResult.build<List<GslEntry>>(logger)
    val indexEntries = parseGslIndex(cursor, cursor.size.toLong())

    val entries = indexEntries.map { entry ->
        cursor.seekStart(entry.offset)
        GslEntry(entry.name, cursor.buffer(entry.size))
    }

    return result.success(entries)
}
