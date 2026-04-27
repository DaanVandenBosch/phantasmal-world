package world.phantasmal.psolib.fileFormats.quest

import mu.KotlinLogging
import world.phantasmal.psolib.asm.BytecodeStringEncoding
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.Cursor
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.encoding.decodeShiftJis
import world.phantasmal.psolib.encoding.encodeShiftJis
import kotlin.math.min

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
    var bytecodeOffset: Int? = null,
    /** Whether DC/GC text fields use Shift-JIS encoding (Japanese). */
    var shiftJis: Boolean = false,
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

val BinFormat.stringEncoding: BytecodeStringEncoding
    get() = when (this) {
        BinFormat.DC_GC -> BytecodeStringEncoding.ASCII
        BinFormat.PC, BinFormat.BB -> BytecodeStringEncoding.UTF16
    }

/**
 * @param shiftJis hint that the file may use Shift-JIS encoding (e.g., from filename `_j` suffix).
 *                 For DC/GC format, Shift-JIS is used when this hint is true OR the language field
 *                 in the header is 0 (Japanese). Has no effect on PC/BB formats which use UTF-16.
 *
 * Note: language field 0 = Japanese (Shift-JIS), non-zero = other language (ASCII).
 */
fun parseBin(cursor: Cursor, shiftJis: Boolean = false): BinFile {
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

    // Store non-standard offset for preservation during round-trip
    val preservedOffset = if (bytecodeOffset != DC_GC_OBJECT_CODE_OFFSET &&
        bytecodeOffset != PC_OBJECT_CODE_OFFSET &&
        bytecodeOffset != BB_OBJECT_CODE_OFFSET
    ) {
        bytecodeOffset
    } else {
        null
    }

    val questId: Int
    val language: Int
    val questName: String
    val shortDescription: String
    val longDescription: String

    if (format == BinFormat.DC_GC) {
        language = cursor.byte().toInt()
        cursor.seek(1) // Skip unknown_a3.
        questId = cursor.short().toInt()

        // language == 0 indicates Japanese — also use Shift-JIS in that case.
        val useShiftJisStrings = shiftJis || language == 0
        if (useShiftJisStrings) {
            questName = readShiftJisString(cursor, 32)
            shortDescription = readShiftJisString(cursor, 128)
            longDescription = readShiftJisString(cursor, 288)
        } else {
            questName = cursor.stringAscii(32, nullTerminated = true, dropRemaining = true)
            shortDescription = cursor.stringAscii(128, nullTerminated = true, dropRemaining = true)
            longDescription = cursor.stringAscii(288, nullTerminated = true, dropRemaining = true)
        }
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

    // Use the header's size field when it's plausible — the actual decompressed data
    // can be larger due to PRS streams lacking a terminator or having trailing bytes.
    val effectiveSize = if (size in 1..cursor.size) size else cursor.size

    if (size != cursor.size) {
        logger.warn { "Value $size in bin size field does not match actual size ${cursor.size}." }
    }

    val shopItems = if (format == BinFormat.BB) {
        cursor.seek(4) // Skip padding.
        cursor.uIntArray(932)
    } else {
        UIntArray(0)
    }

    val labelOffsetCount = (effectiveSize - labelOffsetTableOffset) / 4
    val bytecodeSize = labelOffsetTableOffset - bytecodeOffset
    val labelOffsets = cursor
        .seekStart(labelOffsetTableOffset)
        .intArray(labelOffsetCount)

    // Sanitize label offsets: replace out-of-range values with -1 (unused).
    for (i in labelOffsets.indices) {
        val off = labelOffsets[i]
        if (off != -1 && (off < 0 || off >= bytecodeSize)) {
            labelOffsets[i] = -1
        }
    }

    val bytecode = cursor
        .seekStart(bytecodeOffset)
        .buffer(bytecodeSize)

    val useShiftJis = format == BinFormat.DC_GC && (shiftJis || language == 0)

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
        preservedOffset,
        shiftJis = useShiftJis,
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

    val bytecodeOffset = bin.bytecodeOffset ?: when (bin.format) {
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
        cursor.writeByte(bin.language.toByte())
        cursor.writeByte(0) // unknown_a3
        cursor.writeShort(bin.questId.toShort())

        if (bin.shiftJis) {
            // Japanese: Shift-JIS encoding.
            writeShiftJisString(cursor, bin.questName, 32)
            writeShiftJisString(cursor, bin.shortDescription, 128)
            writeShiftJisString(cursor, bin.longDescription, 288)
        } else {
            cursor.writeStringAscii(bin.questName, 32)
            cursor.writeStringAscii(bin.shortDescription, 128)
            cursor.writeStringAscii(bin.longDescription, 288)
        }
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

/**
 * Reads [byteLength] bytes from [cursor], decodes as Shift-JIS, and strips null terminator.
 */
private fun readShiftJisString(cursor: Cursor, byteLength: Int): String {
    val bytes = cursor.byteArray(byteLength)
    // Find null terminator.
    val end = bytes.indexOf(0)
    val trimmed = if (end >= 0) bytes.copyOf(end) else bytes
    return decodeShiftJis(trimmed)
}

/**
 * Encodes [str] as Shift-JIS and writes exactly [byteLength] bytes to [cursor], padding with nulls.
 */
private fun writeShiftJisString(cursor: world.phantasmal.psolib.cursor.WritableCursor, str: String, byteLength: Int) {
    if (byteLength <= 0) return

    val encoded = encodeShiftJis(str)
    // Reserve at least 1 byte for null terminator.
    var len = min(byteLength - 1, encoded.size)

    // Avoid writing an orphaned Shift-JIS lead byte at the boundary.
    // Lead bytes for 2-byte sequences are in the ranges 0x81–0x9F and 0xE0–0xFC.
    if (len > 0 && len < encoded.size) {
        val lastByte = encoded[len - 1].toInt() and 0xFF
        if (lastByte in 0x81..0x9F || lastByte in 0xE0..0xFC) {
            len-- // Drop the orphaned lead byte; it will be null-padded.
        }
    }

    for (i in 0 until len) {
        cursor.writeByte(encoded[i])
    }
    for (i in len until byteLength) {
        cursor.writeByte(0)
    }
}
