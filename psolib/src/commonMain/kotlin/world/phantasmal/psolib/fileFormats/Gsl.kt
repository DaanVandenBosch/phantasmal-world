package world.phantasmal.psolib.fileFormats

import mu.KotlinLogging
import world.phantasmal.core.PwResult
import world.phantasmal.core.Severity
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.Cursor

private val logger = KotlinLogging.logger {}

private const val ENTRY_SIZE = 0x30
private const val NAME_SIZE = 0x20
private const val PAGE_SIZE = 0x800

class GslEntry(val name: String, val data: Buffer)

/**
 * Parses a GSL archive. GSL is a simple archive format used by PSO for e.g. item parameter tables.
 *
 * Format:
 * - Header entries, each 0x30 bytes: 0x20 bytes ASCII filename, 4 bytes offset (in pages of
 *   0x800), 4 bytes size, 8 bytes unused.
 * - A filename starting with a zero byte marks the end of the header.
 */
fun parseGsl(cursor: Cursor): PwResult<List<GslEntry>> {
    val result = PwResult.build<List<GslEntry>>(logger)
    val entries = mutableListOf<GslEntry>()

    // Parse header entries.
    while (cursor.bytesLeft >= ENTRY_SIZE) {
        // Peek at first byte to check for end-of-header marker.
        val firstByte = cursor.uByte()

        if (firstByte == 0.toUByte()) {
            break
        }

        // Go back one byte and read the full name.
        cursor.seek(-1)

        val name = cursor.stringAscii(NAME_SIZE, nullTerminated = true, dropRemaining = true)
        val offsetPages = cursor.int()
        val size = cursor.int()
        // Skip 8 unused bytes.
        cursor.seek(8)

        val offset = offsetPages * PAGE_SIZE

        when {
            offset > cursor.size -> {
                result.addProblem(
                    Severity.Warning,
                    "GSL entry \"$name\" is invalid.",
                    "Offset $offset (page $offsetPages) is beyond end of file (${cursor.size} bytes).",
                )
            }

            offset + size > cursor.size -> {
                result.addProblem(
                    Severity.Warning,
                    "GSL entry \"$name\" is invalid.",
                    "Data of size $size at offset $offset extends beyond end of file (${cursor.size} bytes).",
                )
            }

            else -> {
                val startPos = cursor.position
                cursor.seekStart(offset)
                entries.add(GslEntry(name, cursor.buffer(size)))
                cursor.seekStart(startPos)
            }
        }
    }

    return result.success(entries)
}
