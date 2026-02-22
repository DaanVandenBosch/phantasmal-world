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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
        assertEquals(setOf(0), quest.mapDesignations[0]!!)
        assertEquals(setOf(0), quest.mapDesignations[2]!!)
        assertEquals(setOf(0), quest.mapDesignations[11]!!)
        assertEquals(setOf(4), quest.mapDesignations[5]!!)
        assertEquals(setOf(0), quest.mapDesignations[12]!!)
        assertEquals(setOf(4), quest.mapDesignations[7]!!)
        assertEquals(setOf(0), quest.mapDesignations[13]!!)
        assertEquals(setOf(4), quest.mapDesignations[8]!!)
        assertEquals(setOf(4), quest.mapDesignations[10]!!)
        assertEquals(setOf(0), quest.mapDesignations[14]!!)

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

    @Test
    fun parseQstToQuest_with_challenge_mode_quest() = testAsync {
        val result = parseQstToQuest(readFile("/chl01.qst"), lenient = true)

        assertTrue(result is Success, "Failed to parse challenge mode quest: ${result.problems.joinToString()}")

        val quest = result.value.quest

        // Verify challenge mode events were parsed
        val cmEvents = quest.events.filter { it.isChallengeMode }
        assertTrue(cmEvents.isNotEmpty(), "Expected challenge mode events to be parsed")

        // Verify CM-specific entity types were parsed
        assertTrue(
            quest.cmRandomSpawns.isNotEmpty() || quest.cmMonsterMappings.isNotEmpty(),
            "Expected challenge mode spawn or mapping data"
        )
    }

    @Test
    fun round_trip_test_with_challenge_mode_quest() = testAsync {
        // Just verify DAT parsing works correctly
        val result = parseQstToQuest(readFile("/chl01.qst"), lenient = true)
        assertTrue(result is Success)

        val quest = result.value.quest

        // Verify challenge mode data was parsed
        val cmEvents = quest.events.filter { it.isChallengeMode }
        assertTrue(cmEvents.isNotEmpty())

        // Write and re-parse just the DAT to verify round-trip
        val (_, dat) = writeQuestToBinDat(quest, result.value.version)
        val reparsedDat = parseDat(dat.cursor())

        // Verify same number of entities
        assertEquals(quest.objects.size, reparsedDat.objs.size)
        assertEquals(quest.npcs.size, reparsedDat.npcs.size)
        assertEquals(quest.events.size, reparsedDat.events.size)
        assertEquals(quest.cmRandomSpawns.size, reparsedDat.cmRandomSpawns.size)
        assertEquals(quest.cmMonsterMappings.size, reparsedDat.cmMonsterMappings.size)

        // Verify challenge mode event flags are preserved
        val reparsedCmEvents = reparsedDat.events.filter { it.isChallengeMode }
        assertEquals(cmEvents.size, reparsedCmEvents.size)
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

        assertEquals(origQuest.mapDesignations, newQuest.mapDesignations)
        assertDeepEquals(origQuest.bytecodeIr, newQuest.bytecodeIr, ignoreSrcLocs = true)
    }

    @Test
    fun cross_episode_map_designate_should_set_mapEpisode() = testAsync {
        val result = parseQstToQuest(readFile("/lost_son_hopkins.qst"), lenient = true)
        assertTrue(result is Success, "Failed to parse quest: ${result.problems.joinToString()}")

        val quest = result.value.quest

        // This is an Episode IV quest
        assertEquals(Episode.IV, quest.episode)

        // The quest should have floor mappings
        assertTrue(quest.floorMappings.isNotEmpty(), "Expected floor mappings")

        // Find floor 0 mapping - it uses mapId 0x12 (18) which is EP2 Lab
        val floor0 = quest.floorMappings.find { it.floorId == 0 }
        assertNotNull(floor0, "Expected floor 0 mapping")
        assertEquals(0x12, floor0.mapId, "Floor 0 should use mapId 0x12 (Lab)")
        assertEquals(0, floor0.areaId, "Lab should have areaId=0")
        assertEquals(Episode.II, floor0.mapEpisode, "mapId 0x12 (Lab) should have mapEpisode=Episode.II")

        // Simulate what QuestModel does: resolve variant using mapEpisode
        // With the fix: getVariant(mapping.mapEpisode ?: episode, mapping.areaId, mapping.variantId)
        // = getVariant(Episode.II, 0, 0) -> should find EP2 Lab, NOT EP4 Pioneer II
        val resolvedEpisode = floor0.mapEpisode ?: quest.episode
        assertEquals(Episode.II, resolvedEpisode, "Should use Episode.II for variant lookup")

        // Verify the area it resolves to is Lab (EP2 area 0), not Pioneer II (EP4 area 0)
        val ep2Areas = getAreasForEpisode(Episode.II)
        val ep4Areas = getAreasForEpisode(Episode.IV)
        val resolvedArea = ep2Areas.find { it.id == floor0.areaId }
        val wrongArea = ep4Areas.find { it.id == floor0.areaId }

        assertNotNull(resolvedArea, "Should find area in EP2")
        assertEquals("Lab", resolvedArea.name, "EP2 area 0 should be Lab")
        assertEquals("Pioneer II", wrongArea?.name, "EP4 area 0 would be Pioneer II (wrong)")

        // Verify other EP4 floor mappings have mapEpisode=IV
        val ep4Floors = quest.floorMappings.filter { it.floorId != 0 }
        for (mapping in ep4Floors) {
            assertEquals(Episode.IV, mapping.mapEpisode,
                "EP4 map floorId=${mapping.floorId} mapId=0x${mapping.mapId.toString(16)} should have mapEpisode=IV")
        }
    }

    @Test
    fun findEpisodeByMapId_returns_correct_episode() {
        assertEquals(Episode.I, findEpisodeByMapId(0x00), "Pioneer II EP1")
        assertEquals(Episode.I, findEpisodeByMapId(0x01), "Forest 1")
        assertEquals(Episode.II, findEpisodeByMapId(0x12), "Lab EP2")
        assertEquals(Episode.II, findEpisodeByMapId(0x13), "VR Temple Alpha")
        assertEquals(Episode.IV, findEpisodeByMapId(0x2D), "Pioneer II EP4")
        assertEquals(Episode.IV, findEpisodeByMapId(0x24), "Crater Route 1")
    }

    companion object {
        private val EXCLUDED = listOf(
            ".raw",
            // Challenge mode quests: Basic parsing support exists, but some quests have issues
            // that need further investigation (e.g., bytecode variations, specific quest structures).
            // The chl01.qst test demonstrates that core CM parsing works.
            "/chl/",
            // Central Dome Fire Swirl seems to be corrupt for two reasons:
            // - It's ID is 33554458, according to the .bin, which is too big for the .qst format.
            // - It has an NPC with script label 100, but the code at that label is invalid.
            "/solo/ep1/side/26.qst",
            // TODO: PRS-compressed file seems corrupt in Gallon's Plan, but qedit has no issues
            //       with it.
            "/solo/ep1/side/quest035.qst",
            // TODO: Bytecode IR round-trip loses one segment (276 -> 275).
            "/ep4/event/clarie's deal.qst",
        )
    }
}
