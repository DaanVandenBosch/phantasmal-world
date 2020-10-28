package world.phantasmal.lib.fileFormats.quest

import mu.KotlinLogging
import world.phantasmal.lib.buffer.Buffer
import world.phantasmal.lib.cursor.Cursor

private val logger = KotlinLogging.logger {}

private const val DC_GC_OBJECT_CODE_OFFSET = 468
private const val PC_OBJECT_CODE_OFFSET = 920
private const val BB_OBJECT_CODE_OFFSET = 4652

class BinFile(
    val format: BinFormat,
    val questId: Int,
    val language: Int,
    val questName: String,
    val shortDescription: String,
    val longDescription: String,
    val objectCode: Buffer,
    val labelOffsets: IntArray,
    val shopItems: UIntArray,
)

enum class BinFormat {
    /**
     * Dreamcast/GameCube
     */
    DC_GC,

    /**
     * Desktop
     */
    PC,

    /**
     * BlueBurst
     */
    BB,
}

fun parseBin(cursor: Cursor): BinFile {
    val objectCodeOffset = cursor.int()
    val labelOffsetTableOffset = cursor.int() // Relative offsets
    val size = cursor.int()
    cursor.seek(4) // Always seems to be 0xFFFFFFFF.

    val format = when (objectCodeOffset) {
        DC_GC_OBJECT_CODE_OFFSET -> BinFormat.DC_GC
        PC_OBJECT_CODE_OFFSET -> BinFormat.PC
        BB_OBJECT_CODE_OFFSET -> BinFormat.BB
        else -> {
            logger.warn {
                "Object code at unexpected offset $objectCodeOffset, assuming file is a PC file."
            }
            BinFormat.PC
        }
    }

    val questId: Int
    val language: Int
    val questName: String
    val shortDescription: String
    val longDescription: String

    if (format == BinFormat.DC_GC) {
        cursor.seek(1)
        language = cursor.byte().toInt()
        questId = cursor.short().toInt()
        questName = cursor.stringAscii(32, nullTerminated = true, dropRemaining = true)
        shortDescription = cursor.stringAscii(128, nullTerminated = true, dropRemaining = true)
        longDescription = cursor.stringAscii(288, nullTerminated = true, dropRemaining = true)
    } else {
        if (format == BinFormat.PC) {
            language = cursor.short().toInt()
            questId = cursor.short().toInt()
        } else {
            questId = cursor.int()
            language = cursor.int()
        }

        questName = cursor.stringUtf16(64, nullTerminated = true, dropRemaining = true)
        shortDescription = cursor.stringUtf16(256, nullTerminated = true, dropRemaining = true)
        longDescription = cursor.stringUtf16(576, nullTerminated = true, dropRemaining = true)
    }

    if (size != cursor.size) {
        logger.warn { "Value $size in bin size field does not match actual size ${cursor.size}." }
    }

    val shopItems = if (format == BinFormat.BB) {
        cursor.seek(4) // Skip padding.
        cursor.uIntArray(932)
    } else {
        UIntArray(0)
    }

    val labelOffsetCount = (cursor.size - labelOffsetTableOffset) / 4
    val labelOffsets = cursor
        .seekStart(labelOffsetTableOffset)
        .intArray(labelOffsetCount)

    val objectCode = cursor
        .seekStart(objectCodeOffset)
        .buffer(labelOffsetTableOffset - objectCodeOffset)

    return BinFile(
        format,
        questId,
        language,
        questName,
        shortDescription,
        longDescription,
        objectCode,
        labelOffsets,
        shopItems,
    )
}
