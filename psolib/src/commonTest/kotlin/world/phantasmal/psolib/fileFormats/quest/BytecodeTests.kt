package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.core.Success
import world.phantasmal.psolib.asm.DataSegment
import world.phantasmal.psolib.asm.InstructionSegment
import world.phantasmal.psolib.asm.OP_BB_MAP_DESIGNATE
import world.phantasmal.psolib.asm.OP_GET_PHYSICAL_DATA
import world.phantasmal.psolib.asm.OP_RET
import world.phantasmal.psolib.asm.OP_SET_EPISODE
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.test.LibTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals
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
            dcGcFormat = false,
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
     * Tests that enemy data referenced by get_physical_data is correctly parsed as a DataSegment
     * instead of being misinterpreted as instructions.
     *
     * Bytecode layout:
     *   Label 0 (offset 0): get_physical_data 1, ret
     *   Label 1 (offset 5): 36 bytes of BattleParamEntry enemy data
     */
    @Test
    fun get_physical_data_references_data_segment() {
        // BattleParamEntry stats data (36 bytes):
        // ATP=140, MST=0, EVP=22, HP=180, DFP=30, ATA=80, LCK=10, ESP=10,
        // float=30.0, float=19.0, level/unk, EXP=40, meseta=0
        val enemyData = ubyteArrayOf(
            0x8Cu, 0x00u, 0x00u, 0x00u, 0x16u, 0x00u, 0xB4u, 0x00u,
            0x1Eu, 0x00u, 0x50u, 0x00u, 0x0Au, 0x00u, 0x0Au, 0x00u,
            0x00u, 0x00u, 0xF0u, 0x41u, 0x00u, 0x00u, 0x98u, 0x41u,
            0x00u, 0x00u, 0x37u, 0x00u, 0x28u, 0x00u, 0x00u, 0x00u,
            0x00u, 0x00u, 0x00u, 0x00u,
        )

        val bytecode = ubyteArrayOf(
            // Label 0: code segment
            0xF8u, 0x92u, 0x01u, 0x00u,  // get_physical_data 1  (opcode F892, label 1 as uint16)
            0x01u,                        // ret
            // Label 1: data segment (enemy data, should NOT be parsed as opcodes)
            *enemyData,
        )

        val buffer = Buffer.fromByteArray(bytecode.toByteArray())

        val result = parseBytecode(
            buffer,
            labelOffsets = intArrayOf(0, 5),  // label 0 → offset 0, label 1 → offset 5
            entryLabels = setOf(0),
            dcGcFormat = false,
            lenient = false,
        )

        assertTrue(result is Success)

        val ir = result.value
        assertEquals(2, ir.segments.size)

        // First segment: instructions
        val codeSegment = ir.segments[0]
        assertTrue(codeSegment is InstructionSegment, "First segment should be InstructionSegment")
        assertEquals(listOf(0), codeSegment.labels)
        assertEquals(2, codeSegment.instructions.size)
        assertEquals(OP_GET_PHYSICAL_DATA, codeSegment.instructions[0].opcode)
        assertEquals(1, codeSegment.instructions[0].args[0].value)
        assertEquals(OP_RET, codeSegment.instructions[1].opcode)

        // Second segment: data (enemy stats), NOT instructions
        val dataSegment = ir.segments[1]
        assertTrue(dataSegment is DataSegment, "Enemy data should be parsed as DataSegment, not InstructionSegment")
        assertEquals(listOf(1), dataSegment.labels)
        assertEquals(enemyData.size, dataSegment.data.size)
    }
}
