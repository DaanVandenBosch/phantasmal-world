package world.phantasmal.lib.fileFormats

import mu.KotlinLogging
import world.phantasmal.core.PwResult
import world.phantasmal.core.Severity
import world.phantasmal.lib.buffer.Buffer
import world.phantasmal.lib.cursor.Cursor

private val logger = KotlinLogging.logger {}

private const val AFS = 0x00534641

/**
 * Returns the files contained in the given AFS archive. AFS is a simple archive format used by SEGA
 * for e.g. player character textures.
 */
fun parseAfs(cursor: Cursor): PwResult<List<Buffer>> {
    val result = PwResult.build<List<Buffer>>(logger)

    if (cursor.bytesLeft < 8) {
        return result
            .addProblem(
                Severity.Error,
                "AFS archive is corrupted.",
                "Expected at least 8 bytes for the header, got ${cursor.bytesLeft} bytes.",
            )
            .failure()
    }

    val magic = cursor.int()

    if (magic != AFS) {
        return result
            .addProblem(Severity.Error, "AFS archive is corrupted.", "Magic bytes not present.")
            .failure()
    }

    val fileCount = cursor.short()

    // Skip two unused bytes (are these just part of the file count field?).
    cursor.seek(2)

    val files = mutableListOf<Buffer>()

    for (i in 1..fileCount) {
        if (cursor.bytesLeft < 8) {
            result.addProblem(
                Severity.Warning,
                "AFS file entry $i is invalid.",
                "Couldn't read file entry $i, only ${cursor.bytesLeft} bytes left.",
            )

            break
        }

        val offset = cursor.int()
        val size = cursor.int()

        when {
            offset > cursor.size -> {
                result.addProblem(
                    Severity.Warning,
                    "AFS file entry $i is invalid.",
                    "Invalid file offset $offset for entry $i.",
                )
            }

            offset + size > cursor.size -> {
                result.addProblem(
                    Severity.Warning,
                    "AFS file entry $i is invalid.",
                    "File size $size (offset: $offset) of entry $i too large.",
                )
            }

            else -> {
                val startPos = cursor.position
                cursor.seekStart(offset)
                files.add(cursor.buffer(size))
                cursor.seekStart(startPos)
            }
        }
    }

    return result.success(files)
}
