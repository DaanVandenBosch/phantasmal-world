package world.phantasmal.lib.fileFormats

import mu.KotlinLogging
import world.phantasmal.core.PwResult
import world.phantasmal.core.Severity
import world.phantasmal.lib.cursor.Cursor

private val logger = KotlinLogging.logger {}

class IffChunk(val type: Int, val data: Cursor)

class IffChunkHeader(val type: Int, val size: Int)

/**
 * PSO uses a little endian variant of the IFF format.
 * IFF files contain chunks preceded by an 8-byte header.
 * The header consists of 4 ASCII characters for the "Type ID" and a 32-bit integer specifying the chunk size.
 */
fun parseIff(cursor: Cursor): PwResult<List<IffChunk>> =
    parse(cursor) { chunkCursor, type, size -> IffChunk(type, chunkCursor.take(size)) }

/**
 * Parses just the chunk headers.
 */
fun parseIffHeaders(cursor: Cursor): PwResult<List<IffChunkHeader>> =
    parse(cursor) { _, type, size -> IffChunkHeader(type, size) }

private fun <T> parse(
    cursor: Cursor,
    getChunk: (Cursor, type: Int, size: Int) -> T,
): PwResult<List<T>> {
    val result = PwResult.build<List<T>>(logger)
    val chunks = mutableListOf<T>()
    var corrupted = false

    while (cursor.bytesLeft >= 8) {
        val type = cursor.i32()
        val sizePos = cursor.position
        val size = cursor.i32()

        if (size > cursor.bytesLeft) {
            corrupted = true
            result.addProblem(
                if (chunks.isEmpty()) Severity.Error else Severity.Warning,
                "IFF file corrupted.",
                "Size $size was too large (only ${cursor.bytesLeft} bytes left) at position $sizePos."
            )

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
