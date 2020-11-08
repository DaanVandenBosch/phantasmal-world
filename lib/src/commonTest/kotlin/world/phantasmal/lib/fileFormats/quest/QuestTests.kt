package world.phantasmal.lib.fileFormats.quest

import world.phantasmal.core.Success
import world.phantasmal.lib.assembly.*
import world.phantasmal.lib.test.LibTestSuite
import world.phantasmal.lib.test.readFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestTests : LibTestSuite() {
    @Test
    fun parseBinDatToQuest_with_towards_the_future() = asyncTest {
        val result = parseBinDatToQuest(readFile("/quest118_e.bin"), readFile("/quest118_e.dat"))

        assertTrue(result is Success)
        assertTrue(result.problems.isEmpty())

        val quest = result.value

        assertEquals("Towards the Future", quest.name)
        assertEquals("Challenge the\nnew simulator.", quest.shortDescription)
        assertEquals(
            "Client: Principal\nQuest: Wishes to have\nhunters challenge the\nnew simulator\nReward: ??? Meseta",
            quest.longDescription
        )
        assertEquals(Episode.I, quest.episode)
        assertEquals(277, quest.objects.size)
        // TODO: Test objects.
//        assertEquals(ObjectType.MenuActivation, quest.objects[0])
//        assertEquals(ObjectType.PlayerSet, quest.objects[4])
        assertEquals(216, quest.npcs.size)
        assertEquals(10, quest.mapDesignations.size)
        assertEquals(0, quest.mapDesignations[0])
        assertEquals(0, quest.mapDesignations[2])
        assertEquals(0, quest.mapDesignations[11])
        assertEquals(4, quest.mapDesignations[5])
        assertEquals(0, quest.mapDesignations[12])
        assertEquals(4, quest.mapDesignations[7])
        assertEquals(0, quest.mapDesignations[13])
        assertEquals(4, quest.mapDesignations[8])
        assertEquals(4, quest.mapDesignations[10])
        assertEquals(0, quest.mapDesignations[14])

        val seg1 = quest.byteCodeIr[0]
        assertTrue(seg1 is InstructionSegment)
        assertTrue(0 in seg1.labels)
        assertEquals(OP_SET_EPISODE, seg1.instructions[0].opcode)
        assertEquals(0, seg1.instructions[0].args[0].value)
        assertEquals(OP_ARG_PUSHL, seg1.instructions[1].opcode)
        assertEquals(0, seg1.instructions[1].args[0].value)
        assertEquals(OP_ARG_PUSHW, seg1.instructions[2].opcode)
        assertEquals(150, seg1.instructions[2].args[0].value)
        assertEquals(OP_SET_FLOOR_HANDLER, seg1.instructions[3].opcode)

        val seg2 = quest.byteCodeIr[1]
        assertTrue(seg2 is InstructionSegment)
        assertTrue(1 in seg2.labels)

        val seg3 = quest.byteCodeIr[2]
        assertTrue(seg3 is InstructionSegment)
        assertTrue(10 in seg3.labels)

        val seg4 = quest.byteCodeIr[3]
        assertTrue(seg4 is InstructionSegment)
        assertTrue(150 in seg4.labels)
        assertEquals(1, seg4.instructions.size)
        assertEquals(OP_SWITCH_JMP, seg4.instructions[0].opcode)
        assertEquals(0, seg4.instructions[0].args[0].value)
        assertEquals(200, seg4.instructions[0].args[1].value)
        assertEquals(201, seg4.instructions[0].args[2].value)
    }
}
