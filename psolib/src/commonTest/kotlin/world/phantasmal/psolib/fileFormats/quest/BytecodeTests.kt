package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.core.Success
import world.phantasmal.psolib.asm.*
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.test.LibTestSuite
import world.phantasmal.psolib.test.assertDeepEquals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BytecodeTests : LibTestSuite {
    @Test
    fun minimal() {
        val buffer = Buffer.fromByteArray(ubyteArrayOf(
            0xF8u, 0xBCu, 0x01u, 0x00u, 0x00u, 0x00u,        // set_episode 1
            0xF9u, 0x51u, 0x03u, 0x15u, 0x00u, 0x02u, 0x00u, // bb_map_designate 3, 21, 2, 0
            0x01u                                            // ret
        ).toByteArray())

        val result = parseBytecode(
            buffer,
            labelOffsets = intArrayOf(0),
            entryLabels = setOf(0),
            stringEncoding = BytecodeStringEncoding.UTF16,
            lenient = false
        )

        assertTrue(result is Success)
        assertTrue(result.problems.isEmpty())

        val ir = result.value
        val segment = ir.segments[0]

        assertTrue(segment is InstructionSegment)
        assertEquals(OP_SET_EPISODE, segment.instructions[0].opcode)
        assertEquals(1, segment.instructions[0].args[0].value)
        assertEquals(OP_BB_MAP_DESIGNATE, segment.instructions[1].opcode)
        assertEquals(3, segment.instructions[1].args[0].value)
        assertEquals(21, segment.instructions[1].args[1].value)
        assertEquals(2, segment.instructions[1].args[2].value)
    }

    /**
     * Minimal parse test with ASCII encoding (DC/GC format).
     */
    @Test
    fun minimal_ascii() {
        val buffer = Buffer.fromByteArray(ubyteArrayOf(
            0xF8u, 0xBCu, 0x01u, 0x00u, 0x00u, 0x00u,        // set_episode 1
            0xF9u, 0x51u, 0x03u, 0x15u, 0x00u, 0x02u, 0x00u, // bb_map_designate 3, 21, 2, 0
            0x01u                                            // ret
        ).toByteArray())

        val result = parseBytecode(
            buffer,
            labelOffsets = intArrayOf(0),
            entryLabels = setOf(0),
            stringEncoding = BytecodeStringEncoding.ASCII,
            lenient = false
        )

        assertTrue(result is Success)

        val ir = result.value
        val segment = ir.segments[0] as InstructionSegment
        assertEquals(OP_SET_EPISODE, segment.instructions[0].opcode)
        assertEquals(1, segment.instructions[0].args[0].value)

        // Write back and verify byte-for-byte round-trip.
        val written = writeBytecode(ir, BytecodeStringEncoding.ASCII).bytecode
        assertDeepEquals(buffer, written)
    }

    /**
     * Tests that enemy data referenced by get_physical_data is correctly parsed as a DataSegment
     * instead of being misinterpreted as instructions.
     */
    @Test
    fun get_physical_data_references_data_segment() {
        val enemyData = ubyteArrayOf(
            0x8Cu, 0x00u, 0x00u, 0x00u, 0x16u, 0x00u, 0xB4u, 0x00u,
            0x1Eu, 0x00u, 0x50u, 0x00u, 0x0Au, 0x00u, 0x0Au, 0x00u,
            0x00u, 0x00u, 0xF0u, 0x41u, 0x00u, 0x00u, 0x98u, 0x41u,
            0x00u, 0x00u, 0x37u, 0x00u, 0x28u, 0x00u, 0x00u, 0x00u,
            0x00u, 0x00u, 0x00u, 0x00u,
        )

        val bytecode = ubyteArrayOf(
            0xF8u, 0x92u, 0x01u, 0x00u,  // get_physical_data 1
            0x01u,                        // ret
            *enemyData,
        )

        val buffer = Buffer.fromByteArray(bytecode.toByteArray())

        val result = parseBytecode(
            buffer,
            labelOffsets = intArrayOf(0, 5),
            entryLabels = setOf(0),
            stringEncoding = BytecodeStringEncoding.UTF16,
            lenient = false,
        )

        assertTrue(result is Success)

        val ir = result.value
        assertEquals(2, ir.segments.size)

        val codeSegment = ir.segments[0]
        assertTrue(codeSegment is InstructionSegment)
        assertEquals(listOf(0), codeSegment.labels)
        assertEquals(2, codeSegment.instructions.size)
        assertEquals(OP_GET_PHYSICAL_DATA, codeSegment.instructions[0].opcode)

        val dataSegment = ir.segments[1]
        assertTrue(dataSegment is DataSegment)
        assertEquals(listOf(1), dataSegment.labels)
        assertEquals(enemyData.size, dataSegment.data.size)
    }

    /**
     * Round-trip test for Pop instruction with int and label push args.
     * Tests both string encodings (no actual strings, but exercises both code paths).
     *
     * set_floor_handler (0x95) = Pop(IntType, ILabelType)
     */
    @Test
    fun round_trip_pop_instruction_with_int_and_label_args() {
        // arg_pushl=0x49, arg_pushw=0x4B
        val bytecode = ubyteArrayOf(
            0x49u, 0x00u, 0x00u, 0x00u, 0x00u, // arg_pushl 0
            0x4Bu, 0x96u, 0x00u,                // arg_pushw 150
            0x95u,                               // set_floor_handler
            0x01u,                               // ret
        )
        val buffer = Buffer.fromByteArray(bytecode.toByteArray())

        for (encoding in BytecodeStringEncoding.entries) {
            val result = parseBytecode(
                buffer.copy(),
                labelOffsets = intArrayOf(0),
                entryLabels = setOf(0),
                stringEncoding = encoding,
                lenient = false,
            )

            assertTrue(result is Success, "Failed to parse with $encoding")

            val ir = result.value
            val segment = ir.segments[0] as InstructionSegment

            // After normalization: push instructions removed, args inlined on Pop.
            assertEquals(2, segment.instructions.size, "Expected set_floor_handler + ret ($encoding)")
            val sfh = segment.instructions[0]
            assertEquals(OP_SET_FLOOR_HANDLER, sfh.opcode)
            assertEquals(0, sfh.args[0].coerceInt())
            assertEquals(150, sfh.args[1].coerceInt())

            // Verify getSize matches original bytecode size.
            assertEquals(bytecode.size, segment.size(encoding), "getSize mismatch ($encoding)")

            // Write back and verify byte-for-byte equality.
            val written = writeBytecode(ir, encoding).bytecode
            assertDeepEquals(buffer, written)
        }
    }

    /**
     * Round-trip test for Pop instruction with a string argument using ASCII encoding (DC/GC).
     *
     * message (0x50) = Pop(IntType, StringType)
     */
    @Test
    fun round_trip_pop_with_string_arg_ascii() {
        // arg_pushl=0x49, arg_pushs=0x4E
        val bytecode = ubyteArrayOf(
            0x49u, 0x00u, 0x00u, 0x00u, 0x00u, // arg_pushl 0
            0x4Eu, 0x48u, 0x69u, 0x00u,         // arg_pushs "Hi\0" (ASCII)
            0x50u,                               // message
            0x01u,                               // ret
        )
        val buffer = Buffer.fromByteArray(bytecode.toByteArray())

        val result = parseBytecode(
            buffer.copy(),
            labelOffsets = intArrayOf(0),
            entryLabels = setOf(0),
            stringEncoding = BytecodeStringEncoding.ASCII,
            lenient = false,
        )

        assertTrue(result is Success)

        val ir = result.value
        val segment = ir.segments[0] as InstructionSegment
        assertEquals(2, segment.instructions.size)

        val msg = segment.instructions[0]
        assertEquals(OP_MESSAGE, msg.opcode)
        assertEquals(0, msg.args[0].coerceInt())
        assertEquals("Hi", msg.args[1].coerceString())

        assertEquals(bytecode.size, segment.size(BytecodeStringEncoding.ASCII))

        val written = writeBytecode(ir, BytecodeStringEncoding.ASCII).bytecode
        assertDeepEquals(buffer, written)
    }

    /**
     * Round-trip test for Pop instruction with a string argument using UTF-16 encoding (PC/BB).
     *
     * message (0x50) = Pop(IntType, StringType)
     */
    @Test
    fun round_trip_pop_with_string_arg_utf16() {
        // arg_pushl=0x49, arg_pushs=0x4E
        val bytecode = ubyteArrayOf(
            0x49u, 0x00u, 0x00u, 0x00u, 0x00u, // arg_pushl 0
            0x4Eu,                               // arg_pushs
            0x48u, 0x00u,                        // 'H' (UTF-16LE)
            0x69u, 0x00u,                        // 'i' (UTF-16LE)
            0x00u, 0x00u,                        // null terminator
            0x50u,                               // message
            0x01u,                               // ret
        )
        val buffer = Buffer.fromByteArray(bytecode.toByteArray())

        val result = parseBytecode(
            buffer.copy(),
            labelOffsets = intArrayOf(0),
            entryLabels = setOf(0),
            stringEncoding = BytecodeStringEncoding.UTF16,
            lenient = false,
        )

        assertTrue(result is Success)

        val ir = result.value
        val segment = ir.segments[0] as InstructionSegment
        assertEquals(2, segment.instructions.size)

        val msg = segment.instructions[0]
        assertEquals(OP_MESSAGE, msg.opcode)
        assertEquals(0, msg.args[0].coerceInt())
        assertEquals("Hi", msg.args[1].coerceString())

        assertEquals(bytecode.size, segment.size(BytecodeStringEncoding.UTF16))

        val written = writeBytecode(ir, BytecodeStringEncoding.UTF16).bytecode
        assertDeepEquals(buffer, written)
    }

    /**
     * Round-trip test for arg_pushr (register reference) targeting a non-register parameter.
     * After normalization, the arg should have isRegRef=true and round-trip to arg_pushr.
     *
     * set_floor_handler (0x95) = Pop(IntType, ILabelType)
     * Using arg_pushr for the IntType param (unusual but valid in PSO bytecode).
     */
    @Test
    fun round_trip_arg_pushr_for_non_register_param() {
        // arg_pushr=0x48, arg_pushw=0x4B
        val bytecode = ubyteArrayOf(
            0x48u, 0x05u,                       // arg_pushr r5 (register reference for IntType param)
            0x4Bu, 0x96u, 0x00u,                // arg_pushw 150
            0x95u,                               // set_floor_handler
            0x01u,                               // ret
        )
        val buffer = Buffer.fromByteArray(bytecode.toByteArray())

        for (encoding in BytecodeStringEncoding.entries) {
            val result = parseBytecode(
                buffer.copy(),
                labelOffsets = intArrayOf(0),
                entryLabels = setOf(0),
                stringEncoding = encoding,
                lenient = false,
            )

            assertTrue(result is Success, "Failed to parse with $encoding")

            val ir = result.value
            val segment = ir.segments[0] as InstructionSegment
            assertEquals(2, segment.instructions.size, "Instruction count ($encoding)")

            val sfh = segment.instructions[0]
            assertEquals(OP_SET_FLOOR_HANDLER, sfh.opcode)
            val firstArg = sfh.args[0] as IntArg
            assertEquals(5, firstArg.value)
            assertTrue(firstArg.isRegRef, "First arg should be marked as isRegRef ($encoding)")

            assertEquals(bytecode.size, segment.size(encoding), "getSize mismatch ($encoding)")

            val written = writeBytecode(ir, encoding).bytecode
            assertDeepEquals(buffer, written)
        }
    }

    /**
     * Round-trip test for arg_pushl pushing a float value.
     * After normalization, the int bits should be converted to FloatArg.
     *
     * particle2 (0xF8F3) = Pop(RegType, IntType, FloatType)
     */
    @Test
    fun round_trip_float_push_normalization() {
        val floatBits = 1.5f.toRawBits() // 0x3FC00000
        val b0 = (floatBits and 0xFF).toUByte()
        val b1 = ((floatBits shr 8) and 0xFF).toUByte()
        val b2 = ((floatBits shr 16) and 0xFF).toUByte()
        val b3 = ((floatBits shr 24) and 0xFF).toUByte()

        // arg_pushb=0x4A, arg_pushl=0x49
        val bytecode = ubyteArrayOf(
            0x4Au, 0x00u,                       // arg_pushb 0 (register for RegType)
            0x49u, 0x01u, 0x00u, 0x00u, 0x00u, // arg_pushl 1 (IntType)
            0x49u, b0, b1, b2, b3,              // arg_pushl 1.5f bits (FloatType)
            0xF8u, 0xF3u,                       // particle2
            0x01u,                               // ret
        )
        val buffer = Buffer.fromByteArray(bytecode.toByteArray())

        for (encoding in BytecodeStringEncoding.entries) {
            val result = parseBytecode(
                buffer.copy(),
                labelOffsets = intArrayOf(0),
                entryLabels = setOf(0),
                stringEncoding = encoding,
                lenient = false,
            )

            assertTrue(result is Success, "Failed to parse with $encoding")

            val ir = result.value
            val segment = ir.segments[0] as InstructionSegment
            assertEquals(2, segment.instructions.size, "Instruction count ($encoding)")

            val particle2 = segment.instructions[0]
            assertEquals(OP_PARTICLE2, particle2.opcode)
            assertEquals(0, particle2.args[0].coerceInt())   // register
            assertEquals(1, particle2.args[1].coerceInt())   // int
            assertEquals(1.5f, particle2.args[2].coerceFloat(), "Float arg ($encoding)")

            assertEquals(bytecode.size, segment.size(encoding), "getSize mismatch ($encoding)")

            val written = writeBytecode(ir, encoding).bytecode
            assertDeepEquals(buffer, written)
        }
    }

    /**
     * Tests getSize/writeBytecode consistency for StringSegment with both encodings.
     * Constructs IR directly (since string segments need SLabel references from instructions
     * to be discovered by the parser).
     */
    @Test
    fun string_segment_getSize_write_consistency() {
        val testString = "Test"

        for (encoding in BytecodeStringEncoding.entries) {
            val ir = BytecodeIr(listOf(
                InstructionSegment(
                    mutableListOf(0),
                    mutableListOf(Instruction(OP_RET, emptyList(), valid = true, srcLoc = null)),
                ),
                StringSegment(
                    mutableListOf(1),
                    testString,
                    bytecodeSize = null,
                ),
            ))

            val expectedStringSize = when (encoding) {
                // "Test" = 4 chars + 1 null = 5 bytes, rounded to 8 (multiple of 4).
                BytecodeStringEncoding.ASCII -> 8
                // "Test" = 4 chars * 2 + 2 null = 10 bytes, rounded to 12 (multiple of 4).
                BytecodeStringEncoding.UTF16 -> 12
            }

            assertEquals(expectedStringSize, ir.segments[1].size(encoding), "StringSegment size ($encoding)")

            // writeBytecode has an internal consistency check that will throw if sizes don't match.
            val result = writeBytecode(ir, encoding)

            // Total = ret(1) + string segment size.
            assertEquals(1 + expectedStringSize, result.bytecode.size, "Total bytecode size ($encoding)")
        }
    }

    /**
     * Tests writeInlineArgs and getSize for the inline StringType code path.
     *
     * No real PSO opcode has an inline (non-Pop) StringType parameter, so we construct a
     * synthetic opcode to exercise this branch.
     */
    @Test
    fun inline_string_arg_getSize_write_consistency() {
        // Synthetic opcode: code 0x02 (unused), one inline StringType param, not Pop.
        val syntheticOpcode = Opcode(
            code = 0x02,
            mnemonic = "test_inline_str",
            doc = null,
            params = listOf(Param(StringType, null, null, read = false, write = false)),
            stack = null,
            varargs = false,
            known = true,
        )

        val testStr = "Hi"

        for (encoding in BytecodeStringEncoding.entries) {
            val ir = BytecodeIr(listOf(
                InstructionSegment(
                    mutableListOf(0),
                    mutableListOf(
                        Instruction(syntheticOpcode, listOf(StringArg(testStr)), valid = true, srcLoc = null),
                    ),
                ),
            ))

            val expectedSize = when (encoding) {
                // opcode(1) + "Hi\0" ASCII = 1 + 3 = 4
                BytecodeStringEncoding.ASCII -> 1 + testStr.length + 1
                // opcode(1) + "Hi\0" UTF-16LE = 1 + 6 = 7
                BytecodeStringEncoding.UTF16 -> 1 + 2 * testStr.length + 2
            }

            val segment = ir.segments[0] as InstructionSegment
            assertEquals(expectedSize, segment.size(encoding), "getSize ($encoding)")

            // writeBytecode's internal check verifies getSize == actual written bytes.
            val result = writeBytecode(ir, encoding)
            assertEquals(expectedSize, result.bytecode.size, "Written size ($encoding)")
        }
    }
}

class IsRegRefLabelTest : LibTestSuite {
    /**
     * Verifies that a Pop instruction's arg passed via arg_pushr (register reference) does NOT
     * get registered as a label in the segment map.
     *
     * Bug: before the fix, isRegRef args were incorrectly treated as label values, causing
     * register numbers to be added to the label→segmentType map in getArgLabelValues.
     *
     * Setup: `arg_pushl 0, arg_pushr r5, set_floor_handler` in bytecode where:
     * - param[0] = int (floor 0)
     * - param[1] = ilabel (handler label) — but passed via reg ref (dynamic, not static)
     * After normalization set_floor_handler has args=[IntArg(0), IntArg(5, isRegRef=true)].
     * The label map should NOT contain register number 5.
     */
    @Test
    fun reg_ref_arg_not_added_to_label_map() {
        // Binary: arg_pushl 0 (0x49 00 00 00 00) + arg_pushr r5 (0x48 05) + set_floor_handler (0x95) + ret (0x01)
        val buffer = Buffer.fromByteArray(byteArrayOf(
            0x49.toByte(), 0x00, 0x00, 0x00, 0x00, // arg_pushl 0  (floor id)
            0x48.toByte(), 0x05,                   // arg_pushr r5 (handler label via register)
            0x95.toByte(),                         // set_floor_handler
            0x01.toByte(),                         // ret
        ))

        val result = parseBytecode(
            buffer,
            labelOffsets = intArrayOf(0),
            entryLabels = setOf(0),
            stringEncoding = BytecodeStringEncoding.UTF16,
            lenient = true,
        )

        assertTrue(result is Success, "Parse failed: ${result.problems.joinToString()}")

        val ir = result.value
        // Only one segment (label 0) should exist.
        // Before the fix a phantom segment for "label 5" (= register number) would be created.
        val allLabels = ir.segments.flatMap { it.labels }.toSet()
        assertTrue(5 !in allLabels, "Register number 5 must not appear as a label (isRegRef bug)")
        assertTrue(0 in allLabels, "Label 0 must still exist")

        // The set_floor_handler instruction should have its args normalized (isRegRef preserved).
        val seg = ir.segments[0] as InstructionSegment
        val sfh = seg.instructions.find { it.opcode == OP_SET_FLOOR_HANDLER }
        assertNotNull(sfh, "set_floor_handler should be present after normalization")
        assertEquals(2, sfh.args.size, "set_floor_handler should have 2 inlined args")
        val labelArg = sfh.args[1] as? IntArg
        assertNotNull(labelArg)
        assertTrue(labelArg.isRegRef, "Second arg should still be a register reference")
        assertEquals(5, labelArg.value)
    }
}
