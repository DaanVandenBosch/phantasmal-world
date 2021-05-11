package world.phantasmal.lib.fileFormats.quest

import world.phantasmal.core.Success
import world.phantasmal.lib.asm.InstructionSegment
import world.phantasmal.lib.asm.OP_BB_MAP_DESIGNATE
import world.phantasmal.lib.asm.OP_SET_EPISODE
import world.phantasmal.lib.buffer.Buffer
import world.phantasmal.lib.test.LibTestSuite
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
}
