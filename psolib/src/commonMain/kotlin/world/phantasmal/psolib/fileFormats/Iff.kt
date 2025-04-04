package world.phantasmal.psolib.fileFormats

import mu.KotlinLogging
import world.phantasmal.core.PwResult
import world.phantasmal.core.Severity
import world.phantasmal.psolib.cursor.Cursor

private val logger = KotlinLogging.logger {}

class IffChunk(val type: Int, val data: Cursor)

class IffChunkHeader(val type: Int, val size: Int)

/**
 * PSO uses a little endian variant of the IFF format.
 * IFF files contain chunks preceded by an 8-byte header.
 * The header consists of 4 ASCII characters for the "Type ID" and a 32-bit integer specifying the chunk size.
 */
fun parseIff(cursor: Cursor, silent: Boolean = false): PwResult<List<IffChunk>> =
    parse(cursor, silent) { chunkCursor, type, size -> IffChunk(type, chunkCursor.take(size)) }

/**
 * Parses just the chunk headers.
 */
fun parseIffHeaders(cursor: Cursor, silent: Boolean = false): PwResult<List<IffChunkHeader>> =
    parse(cursor, silent) { _, type, size -> IffChunkHeader(type, size) }

private fun <T> parse(
    cursor: Cursor,
    silent: Boolean,
    getChunk: (Cursor, type: Int, size: Int) -> T,
): PwResult<List<T>> {
    val result = PwResult.build<List<T>>(logger)
    val chunks = mutableListOf<T>()
    var corrupted = false

    while (cursor.bytesLeft >= 8) {
        val type = cursor.int()
        val sizePos = cursor.position
        val size = cursor.int()

        if (size > cursor.bytesLeft) {
            corrupted = true

            if (!silent) {
                result.addProblem(
                    if (chunks.isEmpty()) Severity.Error else Severity.Warning,
                    "IFF file corrupted.",
                    "Size $size was too large (only ${cursor.bytesLeft} bytes left) at position $sizePos."
                )
            }

            break
        }

        chunks.add(getChunk(cursor, type, size))
    }

    return if (corrupted && chunks.isEmpty()) {
        result.failure()
    } else {
        result.success(chunks)
    }
}
