package world.phantasmal.psolib.fileFormats.quest

import mu.KotlinLogging
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.Cursor
import world.phantasmal.psolib.cursor.cursor

private val logger = KotlinLogging.logger {}

private const val DC_GC_OBJECT_CODE_OFFSET = 468
private const val PC_OBJECT_CODE_OFFSET = 920
private const val BB_OBJECT_CODE_OFFSET = 4652

class BinFile(
    var format: BinFormat,
    var questId: Int,
    var language: Int,
    var questName: String,
    var shortDescription: String,
    var longDescription: String,
    val bytecode: Buffer,
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
    val bytecodeOffset = cursor.int()
    val labelOffsetTableOffset = cursor.int() // Relative offsets
    val size = cursor.int()
    cursor.seek(4) // Always seems to be 0xFFFFFFFF.

    val format = when (bytecodeOffset) {
        DC_GC_OBJECT_CODE_OFFSET -> BinFormat.DC_GC
        PC_OBJECT_CODE_OFFSET -> BinFormat.PC
        BB_OBJECT_CODE_OFFSET -> BinFormat.BB
        else -> {
            logger.warn {
                "Byte code at unexpected offset $bytecodeOffset, assuming file is a PC file."
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

    val bytecode = cursor
        .seekStart(bytecodeOffset)
        .buffer(labelOffsetTableOffset - bytecodeOffset)

    return BinFile(
        format,
        questId,
        language,
        questName,
        shortDescription,
        longDescription,
        bytecode,
        labelOffsets,
        shopItems,
    )
}

fun writeBin(bin: BinFile): Buffer {
    require(bin.questName.length <= 32) {
        "questName can't be longer than 32 characters, was ${bin.questName.length}"
    }
    require(bin.shortDescription.length <= 127) {
        "shortDescription can't be longer than 127 characters, was ${bin.shortDescription.length}"
    }
    require(bin.longDescription.length <= 287) {
        "longDescription can't be longer than 287 characters, was ${bin.longDescription.length}"
    }
    require(bin.shopItems.isEmpty() || bin.format == BinFormat.BB) {
        "shopItems is only supported in BlueBurst quests."
    }
    require(bin.shopItems.size <= 932) {
        "shopItems can't be larger than 932, was ${bin.shopItems.size}."
    }

    val bytecodeOffset = when (bin.format) {
        BinFormat.DC_GC -> DC_GC_OBJECT_CODE_OFFSET
        BinFormat.PC -> PC_OBJECT_CODE_OFFSET
        BinFormat.BB -> BB_OBJECT_CODE_OFFSET
    }

    val fileSize = bytecodeOffset + bin.bytecode.size + 4 * bin.labelOffsets.size
    val buffer = Buffer.withCapacity(fileSize)
    val cursor = buffer.cursor()

    cursor.writeInt(bytecodeOffset)
    cursor.writeInt(bytecodeOffset + bin.bytecode.size) // Label table offset.
    cursor.writeInt(fileSize)
    cursor.writeInt(-1)

    if (bin.format == BinFormat.DC_GC) {
        cursor.writeByte(0)
        cursor.writeByte(bin.language.toByte())
        cursor.writeShort(bin.questId.toShort())
        cursor.writeStringAscii(bin.questName, 32)
        cursor.writeStringAscii(bin.shortDescription, 128)
        cursor.writeStringAscii(bin.longDescription, 288)
    } else {
        if (bin.format == BinFormat.PC) {
            cursor.writeShort(bin.language.toShort())
            cursor.writeShort(bin.questId.toShort())
        } else {
            cursor.writeInt(bin.questId)
            cursor.writeInt(bin.language)
        }

        cursor.writeStringUtf16(bin.questName, 64)
        cursor.writeStringUtf16(bin.shortDescription, 256)
        cursor.writeStringUtf16(bin.longDescription, 576)
    }

    if (bin.format == BinFormat.BB) {
        cursor.writeInt(0)
        cursor.writeUIntArray(bin.shopItems)

        repeat(932 - bin.shopItems.size) {
            cursor.writeUInt(0u)
        }
    }

    check(cursor.position == bytecodeOffset) {
        "Expected to write $bytecodeOffset bytes before bytecode, but wrote ${cursor.position}."
    }

    cursor.writeCursor(bin.bytecode.cursor())

    cursor.writeIntArray(bin.labelOffsets)

    check(cursor.position == fileSize) {
        "Expected to write $fileSize bytes, but wrote ${cursor.position}."
    }

    return buffer
}
