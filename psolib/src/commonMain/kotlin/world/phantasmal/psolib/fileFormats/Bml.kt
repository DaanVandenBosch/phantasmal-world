package world.phantasmal.psolib.fileFormats

import mu.KotlinLogging
import world.phantasmal.core.PwResult
import world.phantasmal.core.Severity
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.Cursor

private val logger = KotlinLogging.logger {}

private const val HEADER_SIZE = 0x40
private const val ENTRY_HEADER_SIZE = 0x40
private const val NAME_SIZE = 0x20
private const val DATA_ALIGNMENT = 0x20
private const val DATA_START = 0x800

class BmlEntry(
    val name: String,
    /** PRS-compressed model data (NJ/XJ). */
    val data: Buffer,
    /** PRS-compressed texture data (XVM). May be an empty buffer if the entry has no textures. */
    val textureData: Buffer,
)

/**
 * Parses a BML model container. BML is a format used by PSO to bundle compressed model and texture
 * data.
 *
 * Format:
 * - Header: 0x40 bytes (4 unknown, 4 num_entries, 0x38 unknown).
 * - Entry headers: each 0x40 bytes (0x20 filename, 4 compressed_size, 4 unknown,
 *   4 decompressed_size, 4 compressed_gvm_size, 4 decompressed_gvm_size, 0x0C unknown).
 * - Data starts at offset 0x800, each entry's data is aligned to 0x20 bytes.
 * - Each entry contains model data followed by texture data, both PRS-compressed.
 *
 * The returned [BmlEntry.data] and [BmlEntry.textureData] are the raw PRS-compressed buffers.
 * Use [world.phantasmal.psolib.compression.prs.prsDecompress] to decompress them.
 */
fun parseBml(cursor: Cursor): PwResult<List<BmlEntry>> {
    val result = PwResult.build<List<BmlEntry>>(logger)

    if (cursor.bytesLeft < HEADER_SIZE) {
        return result
            .addProblem(
                Severity.Error,
                "BML file is corrupted.",
                "Expected at least $HEADER_SIZE bytes for the header, got ${cursor.bytesLeft} bytes.",
            )
            .failure()
    }

    // Parse header.
    cursor.seek(4) // Skip unknown.
    val numEntries = cursor.int()
    cursor.seek(0x38) // Skip rest of header.

    if (numEntries < 0 || numEntries > 10000) {
        return result
            .addProblem(Severity.Error, "BML file is corrupted.", "Invalid entry count: $numEntries.")
            .failure()
    }

    // Parse entry headers.
    data class EntryHeader(
        val name: String,
        val compressedSize: Int,
        val compressedGvmSize: Int,
    )

    val entryHeaders = mutableListOf<EntryHeader>()

    for (i in 0 until numEntries) {
        if (cursor.bytesLeft < ENTRY_HEADER_SIZE) {
            result.addProblem(
                Severity.Warning,
                "BML entry header $i is invalid.",
                "Couldn't read entry header $i, only ${cursor.bytesLeft} bytes left.",
            )
            break
        }

        val name = cursor.stringAscii(NAME_SIZE, nullTerminated = true, dropRemaining = true)
        val compressedSize = cursor.int()
        cursor.seek(4) // Skip unknown.
        cursor.seek(4) // Skip decompressed_size.
        val compressedGvmSize = cursor.int()
        cursor.seek(4) // Skip decompressed_gvm_size.
        cursor.seek(0x0C) // Skip unknown.

        entryHeaders.add(EntryHeader(name, compressedSize, compressedGvmSize))
    }

    // Data starts at DATA_START (0x800) OR after all entry headers, whichever is later.
    // BML files with >30 entries (>30 × 0x40 = 0x780 bytes of headers) push data past 0x800.
    val dataStart = maxOf(DATA_START, HEADER_SIZE + numEntries * ENTRY_HEADER_SIZE)
    var dataOffset = dataStart
    val entries = mutableListOf<BmlEntry>()

    for ((i, header) in entryHeaders.withIndex()) {
        // Read model data.
        if (dataOffset + header.compressedSize > cursor.size) {
            result.addProblem(
                Severity.Warning,
                "BML entry \"${header.name}\" is invalid.",
                "Model data of size ${header.compressedSize} at offset $dataOffset extends beyond end of file (${cursor.size} bytes).",
            )
            break
        }

        cursor.seekStart(dataOffset)
        val modelData = cursor.buffer(header.compressedSize)

        // Advance past model data, aligned to DATA_ALIGNMENT.
        dataOffset += alignUp(header.compressedSize, DATA_ALIGNMENT)

        // Read texture data.
        val textureData: Buffer

        if (header.compressedGvmSize > 0) {
            if (dataOffset + header.compressedGvmSize > cursor.size) {
                result.addProblem(
                    Severity.Warning,
                    "BML entry \"${header.name}\" is invalid.",
                    "Texture data of size ${header.compressedGvmSize} at offset $dataOffset extends beyond end of file (${cursor.size} bytes).",
                )
                break
            }

            cursor.seekStart(dataOffset)
            textureData = cursor.buffer(header.compressedGvmSize)

            // Advance past texture data, aligned to DATA_ALIGNMENT.
            dataOffset += alignUp(header.compressedGvmSize, DATA_ALIGNMENT)
        } else {
            textureData = Buffer.withSize(0)
        }

        entries.add(BmlEntry(header.name, modelData, textureData))
    }

    return result.success(entries)
}

private fun alignUp(value: Int, alignment: Int): Int {
    val mask = alignment - 1
    return (value + mask) and mask.inv()
}
