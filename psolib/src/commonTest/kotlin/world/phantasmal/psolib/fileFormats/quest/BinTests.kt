package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.test.LibTestSuite
import world.phantasmal.psolib.test.assertDeepEquals
import world.phantasmal.psolib.test.readFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BinTests : LibTestSuite {
    @Test
    fun parse_quest_towards_the_future() = testAsync {
        val bin = parseBin(readFile("/towards_the_future_decompressed.bin"))

        assertEquals(BinFormat.BB, bin.format)
        assertEquals(118, bin.questId)
        assertEquals(0, bin.language)
        assertEquals("Towards the Future", bin.questName)
        assertEquals("Challenge the\nnew simulator.", bin.shortDescription)
        assertEquals(
            "Client: Principal\nQuest: Wishes to have\nhunters challenge the\nnew simulator\nReward: ??? Meseta",
            bin.longDescription
        )
    }

    @Test
    fun parse_and_write_towards_the_future() = parseAndWriteQuest("/towards_the_future_decompressed.bin")

    @Test
    fun parse_and_write_seat_of_the_heart() = parseAndWriteQuest("/seat_of_the_heart_decompressed.bin")

    /**
     * Verify DC/GC bin header reads language byte at offset 16 (before unknown_a3),
     * not swapped. language=0 triggers Shift-JIS string decoding.
     */
    @Test
    fun parse_dc_gc_language_byte_order() {
        val bytecodeOffset = 468 // DC_GC_OBJECT_CODE_OFFSET
        // Minimal bytecode: a single ret (0x49) instruction, plus a label offset table entry.
        val bytecodeSize = 2
        val labelCount = 1
        val fileSize = bytecodeOffset + bytecodeSize + 4 * labelCount

        val buf = Buffer.withSize(fileSize)
        val cursor = buf.cursor()

        // Header
        cursor.writeInt(bytecodeOffset)                    // bytecodeOffset
        cursor.writeInt(bytecodeOffset + bytecodeSize)     // labelOffsetTableOffset
        cursor.writeInt(fileSize)                          // size
        cursor.writeInt(-1)                                // 0xFFFFFFFF

        // DC/GC header fields:
        cursor.writeByte(1)    // language = 1 (non-Japanese, ASCII)
        cursor.writeByte(0)    // unknown_a3
        cursor.writeShort(42)  // questId

        // Quest name (32 bytes ASCII): "TestQ"
        cursor.writeStringAscii("TestQ", 32)
        // Short description (128 bytes)
        cursor.writeStringAscii("Short", 128)
        // Long description (288 bytes)
        cursor.writeStringAscii("Long", 288)

        // Fill remaining header up to bytecodeOffset
        cursor.seekStart(bytecodeOffset)

        // Bytecode: ret instruction (opcode 0x49)
        cursor.writeByte(0x49)
        cursor.writeByte(0x00)

        // Label offset table: label 0 -> offset 0
        cursor.writeInt(0)

        val bin = parseBin(buf.cursor())
        assertEquals(BinFormat.DC_GC, bin.format)
        assertEquals(1, bin.language)
        assertEquals(42, bin.questId)
        assertEquals("TestQ", bin.questName)
    }

    /**
     * Verify DC/GC bin with language=0 round-trips correctly through Shift-JIS encoding.
     * language=0 means Japanese — parseBin should activate Shift-JIS decoding for strings.
     */
    @Test
    fun parse_dc_gc_language_0_uses_shift_jis() {
        val original = BinFile(
            format = BinFormat.DC_GC,
            questId = 1,
            language = 0,
            questName = "Test",
            shortDescription = "Desc",
            longDescription = "Long",
            bytecode = Buffer.withSize(2).also { it.setByte(0, 0x49) }, // ret
            labelOffsets = intArrayOf(0),
            shopItems = UIntArray(0),
            shiftJis = true,
        )

        val written = writeBin(original)
        val parsed = parseBin(written.cursor())

        assertEquals(BinFormat.DC_GC, parsed.format)
        assertEquals(0, parsed.language)
        assertEquals(1, parsed.questId)
        assertEquals("Test", parsed.questName)
        assertEquals("Desc", parsed.shortDescription)
        assertTrue(parsed.shiftJis, "language=0 should activate Shift-JIS")
    }

    /**
     * Verify that when the bin size field is smaller than actual cursor size,
     * effectiveSize is used for label offset count, and out-of-range label offsets
     * are sanitized to -1.
     */
    @Test
    fun parse_bin_with_oversized_data_sanitizes_labels() {
        val bytecodeOffset = 468
        val bytecodeSize = 4
        val labelCount = 2
        val declaredSize = bytecodeOffset + bytecodeSize + 4 * labelCount
        // Actual buffer is larger (simulating PRS decompression producing extra bytes)
        val extraBytes = 20
        val actualSize = declaredSize + extraBytes

        val buf = Buffer.withSize(actualSize)
        val cursor = buf.cursor()

        cursor.writeInt(bytecodeOffset)
        cursor.writeInt(bytecodeOffset + bytecodeSize)
        cursor.writeInt(declaredSize) // declared size < actual size
        cursor.writeInt(-1)

        // DC/GC header
        cursor.writeByte(1)
        cursor.writeByte(0)
        cursor.writeShort(1)
        cursor.writeStringAscii("Q", 32)
        cursor.writeStringAscii("", 128)
        cursor.writeStringAscii("", 288)

        cursor.seekStart(bytecodeOffset)
        // Bytecode: 4 bytes
        cursor.writeByte(0x49)
        cursor.writeByte(0x00)
        cursor.writeByte(0x00)
        cursor.writeByte(0x00)

        // Label offsets: one valid (0), one out-of-range (9999)
        cursor.writeInt(0)
        cursor.writeInt(9999)

        // Fill extra bytes (garbage from PRS)
        for (i in 0 until extraBytes) {
            cursor.writeByte(0xFF.toByte())
        }

        val bin = parseBin(buf.cursor())
        // Should use declaredSize for label count, so only 2 labels (not more from extra bytes)
        assertEquals(labelCount, bin.labelOffsets.size)
        // First label valid
        assertEquals(0, bin.labelOffsets[0])
        // Second label out-of-range, should be sanitized to -1
        assertEquals(-1, bin.labelOffsets[1])
    }

    private fun parseAndWriteQuest(file: String) = testAsync {
        val origBin = readFile(file)
        val newBin = writeBin(parseBin(origBin)).cursor()
        origBin.seekStart(0)

        assertDeepEquals(origBin, newBin)
    }
}
