package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.core.Success
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.asm.*
import world.phantasmal.psolib.cursor.Cursor
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.test.LibTestSuite
import world.phantasmal.psolib.test.assertDeepEquals
import world.phantasmal.psolib.test.readFile
import world.phantasmal.psolib.test.testWithTetheallaQuests
import world.phantasmal.testUtils.assertDeepEquals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestTests : LibTestSuite {
    @Test
    fun parseBinDatToQuest_with_towards_the_future() = testAsync {
        val result = parseBinDatToQuest(readFile("/quest118_e.bin"), readFile("/quest118_e.dat"))

        assertTrue(result is Success)
        assertTrue(result.problems.isEmpty())

        testTowardsTheFutureParseResult(result.value)
    }

    @Test
    fun parseQstToQuest_with_towards_the_future() = testAsync {
        val result = parseQstToQuest(readFile("/quest118_e.qst"))

        assertTrue(result is Success)
        assertTrue(result.problems.isEmpty())

        assertEquals(Version.BB, result.value.version)
        assertTrue(result.value.online)

        testTowardsTheFutureParseResult(result.value.quest)
    }

    private fun testTowardsTheFutureParseResult(quest: Quest) {
        assertEquals("Towards the Future", quest.name)
        assertEquals("Challenge the\nnew simulator.", quest.shortDescription)
        assertEquals(
            "Client: Principal\nQuest: Wishes to have\nhunters challenge the\nnew simulator\nReward: ??? Meseta",
            quest.longDescription
        )
        assertEquals(Episode.I, quest.episode)
        assertEquals(277, quest.objects.size)
        assertEquals(ObjectType.MenuActivation, quest.objects[0].type)
        assertEquals(ObjectType.PlayerSet, quest.objects[4].type)
        assertEquals(216, quest.npcs.size)
        assertEquals(10, quest.mapDesignations.size)
        assertEquals<Set<Int>>(setOf(0), quest.mapDesignations[0]!!)
        assertEquals<Set<Int>>(setOf(0), quest.mapDesignations[2]!!)
        assertEquals<Set<Int>>(setOf(0), quest.mapDesignations[11]!!)
        assertEquals<Set<Int>>(setOf(4), quest.mapDesignations[5]!!)
        assertEquals<Set<Int>>(setOf(0), quest.mapDesignations[12]!!)
        assertEquals<Set<Int>>(setOf(4), quest.mapDesignations[7]!!)
        assertEquals<Set<Int>>(setOf(0), quest.mapDesignations[13]!!)
        assertEquals<Set<Int>>(setOf(4), quest.mapDesignations[8]!!)
        assertEquals<Set<Int>>(setOf(4), quest.mapDesignations[10]!!)
        assertEquals<Set<Int>>(setOf(0), quest.mapDesignations[14]!!)

        val seg1 = quest.bytecodeIr.segments[0]
        assertTrue(seg1 is InstructionSegment)
        assertTrue(0 in seg1.labels)
        assertEquals(OP_SET_EPISODE, seg1.instructions[0].opcode)
        assertEquals(0, seg1.instructions[0].args[0].value)
        assertEquals(OP_ARG_PUSHL, seg1.instructions[1].opcode)
        assertEquals(0, seg1.instructions[1].args[0].value)
        assertEquals(OP_ARG_PUSHW, seg1.instructions[2].opcode)
        assertEquals(150, seg1.instructions[2].args[0].value)
        assertEquals(OP_SET_FLOOR_HANDLER, seg1.instructions[3].opcode)

        val seg2 = quest.bytecodeIr.segments[1]
        assertTrue(seg2 is InstructionSegment)
        assertTrue(1 in seg2.labels)

        val seg3 = quest.bytecodeIr.segments[2]
        assertTrue(seg3 is InstructionSegment)
        assertTrue(10 in seg3.labels)

        val seg4 = quest.bytecodeIr.segments[3]
        assertTrue(seg4 is InstructionSegment)
        assertTrue(150 in seg4.labels)
        assertEquals(1, seg4.instructions.size)
        assertEquals(OP_SWITCH_JMP, seg4.instructions[0].opcode)
        assertEquals(0, seg4.instructions[0].args[0].value)
        assertEquals(200, seg4.instructions[0].args[1].value)
        assertEquals(201, seg4.instructions[0].args[2].value)
    }

    @Test
    fun round_trip_test_with_towards_the_future() = testAsync {
        val filename = "quest118_e.qst"
        roundTripTest(filename, readFile("/$filename"))
    }

    @Test
    fun round_trip_test_with_seat_of_the_heart() = testAsync {
        val filename = "quest27_e.qst"
        roundTripTest(filename, readFile("/$filename"))
    }

    @Test
    fun round_trip_test_with_lost_head_sword_gc() = testAsync {
        val filename = "lost_heat_sword_gc.qst"
        roundTripTest(filename, readFile("/$filename"))
    }

    // TODO: Figure out why this test is so slow in JS/Karma.
    @Test
    fun round_trip_test_with_all_tethealla_quests() = testAsync(slow = true) {
        testWithTetheallaQuests { path, filename ->
            if (EXCLUDED.any { it in path }) return@testWithTetheallaQuests

            try {
                roundTripTest(filename, readFile(path))
            } catch (e: Throwable) {
                throw Exception("""Failed for "$path": ${e.message}""", e)
            }
        }
    }

    /**
     * Parse a QST file, write the resulting Quest object to QST again, then parse that again.
     * Then check whether the two Quest objects are deeply equal.
     */
    private fun roundTripTest(filename: String, contents: Cursor) {
        val origQuestData = parseQstToQuest(contents).unwrap()
        val origQuest = origQuestData.quest
        val newQuestData = parseQstToQuest(
            writeQuestToQst(
                origQuest,
                filename,
                origQuestData.version,
                origQuestData.online,
            ).cursor()
        ).unwrap()
        val newQuest = newQuestData.quest

        assertEquals(origQuestData.version, newQuestData.version)
        assertEquals(origQuestData.online, newQuestData.online)

        assertEquals(origQuest.name, newQuest.name)
        assertEquals(origQuest.shortDescription, newQuest.shortDescription)
        assertEquals(origQuest.longDescription, newQuest.longDescription)
        assertEquals(origQuest.episode, newQuest.episode)
        assertEquals(origQuest.objects.size, newQuest.objects.size)

        for (i in origQuest.objects.indices) {
            val origObj = origQuest.objects[i]
            val newObj = newQuest.objects[i]
            assertEquals(origObj.areaId, newObj.areaId)
            assertEquals(origObj.sectionId, newObj.sectionId)
            assertEquals(origObj.position, newObj.position)
            assertEquals(origObj.type, newObj.type)
        }

        assertEquals(origQuest.npcs.size, newQuest.npcs.size)

        for (i in origQuest.npcs.indices) {
            val origNpc = origQuest.npcs[i]
            val newNpc = newQuest.npcs[i]
            assertEquals(origNpc.areaId, newNpc.areaId)
            assertEquals(origNpc.sectionId, newNpc.sectionId)
            assertEquals(origNpc.position, newNpc.position)
            assertEquals(origNpc.type, newNpc.type)
        }

        assertDeepEquals(origQuest.mapDesignations, newQuest.mapDesignations, ::assertEquals)
        assertDeepEquals(origQuest.bytecodeIr, newQuest.bytecodeIr, ignoreSrcLocs = true)
    }

    companion object {
        private val EXCLUDED = listOf(
            ".raw",
            // TODO: Test challenge mode quests when they're supported.
            "/chl/",
            // Central Dome Fire Swirl seems to be corrupt for two reasons:
            // - It's ID is 33554458, according to the .bin, which is too big for the .qst format.
            // - It has an NPC with script label 100, but the code at that label is invalid.
            "/solo/ep1/side/26.qst",
            // TODO: PRS-compressed file seems corrupt in Gallon's Plan, but qedit has no issues
            //       with it.
            "/solo/ep1/side/quest035.qst",
        )
    }
}
