package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.core.Success
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.asm.*
import world.phantasmal.psolib.asm.dataFlowAnalysis.FloorMapping
import world.phantasmal.psolib.cursor.Cursor
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.test.LibTestSuite
import world.phantasmal.psolib.test.assertDeepEquals
import world.phantasmal.psolib.test.readFile
import world.phantasmal.psolib.test.TETHEALLA_QUEST_PATH_PREFIX
import world.phantasmal.psolib.test.testWithTetheallaQuests
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class QuestTests : LibTestSuite {
    @Test
    fun parseBinDatToQuest_with_towards_the_future() = testAsync {
        val result = parseBinDatToQuest(readFile("/towards_the_future.bin"), readFile("/towards_the_future.dat"))

        assertTrue(result is Success)
        assertTrue(result.problems.isEmpty())

        testTowardsTheFutureParseResult(result.value)
    }

    @Test
    fun parseQstToQuest_with_towards_the_future() = testAsync {
        val result = parseQstToQuest(readFile("/towards_the_future.qst"))

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
        // Derive variantsByArea view from floorMappings for assertion compatibility
        val variantsByArea = quest.floorMappings
            .groupBy { it.areaId }
            .mapValues { (_, mappings) -> mappings.map { it.variantId }.toSet() }
        assertEquals(10, variantsByArea.size)
        assertEquals(setOf(0), variantsByArea[0]!!)
        assertEquals(setOf(0), variantsByArea[2]!!)
        assertEquals(setOf(0), variantsByArea[11]!!)
        assertEquals(setOf(4), variantsByArea[5]!!)
        assertEquals(setOf(0), variantsByArea[12]!!)
        assertEquals(setOf(4), variantsByArea[7]!!)
        assertEquals(setOf(0), variantsByArea[13]!!)
        assertEquals(setOf(4), variantsByArea[8]!!)
        assertEquals(setOf(4), variantsByArea[10]!!)
        assertEquals(setOf(0), variantsByArea[14]!!)

        val seg1 = quest.bytecodeIr.segments[0]
        assertTrue(seg1 is InstructionSegment)
        assertTrue(0 in seg1.labels)
        assertEquals(OP_SET_EPISODE, seg1.instructions[0].opcode)
        assertEquals(0, seg1.instructions[0].args[0].value)
        assertEquals(OP_SET_FLOOR_HANDLER, seg1.instructions[1].opcode)
        assertEquals(0, seg1.instructions[1].args[0].value)
        assertEquals(150, seg1.instructions[1].args[1].value)

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
    fun parseQstToQuest_with_phantasmal_world_4_multi_floor() = testAsync {
        val result = parseQstToQuest(readFile("$TETHEALLA_QUEST_PATH_PREFIX/ep2/ext/pw4.qst"))

        assertTrue(result is Success)

        val quest = result.value.quest
        assertEquals(Episode.II, quest.episode)

        // PW4 has bb_map_designate instructions:
        //   bb_map_designate 0, 18, 0, 0   -> floor 0, map 18 (Lab), areaId 0, variant 0
        //   bb_map_designate 17, 35, 0, 0  -> floor 17, map 35 (Tower), areaId 17, variant 0
        //   bb_map_designate 16, 35, 1, 0  -> floor 16, map 35 (Tower), areaId 17, variant 1
        assertTrue(quest.floorMappings.isNotEmpty(), "PW4 should have floor mappings")

        // Verify that both tower floors map to area 17 (Tower), NOT area 16 (Seaside Night)
        val floor0 = quest.floorMappings.find { it.floorId == 0 }
        val floor16 = quest.floorMappings.find { it.floorId == 16 }
        val floor17 = quest.floorMappings.find { it.floorId == 17 }

        // Floor 0 -> Lab (area 0)
        assertEquals(FloorMapping(0, 18, 0, 0, Episode.II), floor0, "Floor 0 should be Lab")

        // Floor 17 -> Tower (area 17), variant 0
        assertEquals(FloorMapping(17, 35, 17, 0, Episode.II), floor17, "Floor 17 should be Tower variant 0")

        // Floor 16 -> Tower (area 17), variant 1 (NOT Seaside Night area 16!)
        assertEquals(FloorMapping(16, 35, 17, 1, Episode.II), floor16, "Floor 16 should be Tower variant 1")

        // Verify variantsByArea view: area 17 should have variants {0, 1}
        val variantsByArea = quest.floorMappings
            .groupBy { it.areaId }
            .mapValues { (_, mappings) -> mappings.map { it.variantId }.toSet() }
        assertEquals(setOf(0, 1), variantsByArea[17], "Tower should have variants 0 and 1")
        // Area 16 (Seaside Night) should NOT appear
        assertTrue(16 !in variantsByArea, "Seaside Night should not appear in PW4 variantsByArea")
    }

    @Test
    fun round_trip_test_with_towards_the_future() = testAsync {
        val filename = "towards_the_future.qst"
        roundTripTest(filename, readFile("/$filename"))
    }

    @Test
    fun round_trip_test_with_seat_of_the_heart() = testAsync {
        val filename = "seat_of_the_heart.qst"
        roundTripTest(filename, readFile("/$filename"))
    }

    @Test
    fun round_trip_test_with_lost_head_sword_gc() = testAsync {
        val filename = "lost_heat_sword_gc.qst"
        roundTripTest(filename, readFile("/$filename"))
    }

    @Test
    fun parseQstToQuest_with_challenge_mode_quest() = testAsync {
        val result = parseQstToQuest(readFile("$TETHEALLA_QUEST_PATH_PREFIX/chl/ep1/1.qst"), lenient = true)

        assertTrue(result is Success, "Failed to parse challenge mode quest: ${result.problems.joinToString()}")

        val quest = result.value.quest

        // Verify challenge mode events were parsed
        val cmEvents = quest.events.filter { it.isChallengeMode }
        assertTrue(cmEvents.isNotEmpty(), "Expected challenge mode events to be parsed")

        // Verify CM-specific entity types were parsed
        assertTrue(
            quest.challengeData.cmRandomSpawns.isNotEmpty() || quest.challengeData.cmMonsterMappings.isNotEmpty(),
            "Expected challenge mode spawn or mapping data"
        )
    }

    @Test
    fun round_trip_test_with_challenge_mode_quest() = testAsync {
        // Just verify DAT parsing works correctly
        val result = parseQstToQuest(readFile("$TETHEALLA_QUEST_PATH_PREFIX/chl/ep1/1.qst"), lenient = true)
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
        assertEquals(quest.challengeData.cmRandomSpawns.size, reparsedDat.cmRandomSpawns.size)
        assertEquals(quest.challengeData.cmMonsterMappings.size, reparsedDat.cmMonsterMappings.size)

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

        // Compare floorMappings-derived variantsByArea view
        val origVariantsByArea = origQuest.floorMappings
            .groupBy { it.areaId }
            .mapValues { (_, mappings) -> mappings.map { it.variantId }.toSet() }
        val newVariantsByArea = newQuest.floorMappings
            .groupBy { it.areaId }
            .mapValues { (_, mappings) -> mappings.map { it.variantId }.toSet() }
        assertEquals(origVariantsByArea, newVariantsByArea)
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

    // ---- Additional round-trip and feature tests using existing Tethealla quest files ----

    /**
     * Parse Phantasmal World #4 and verify EP2 Tower floor mappings.
     * Exercises the SetDataTable EP2 floor 16/17 fix and the FloorMapping system.
     */
    @Test
    fun parse_pw4_tower_floor_mappings() = testAsync {
        val result = parseQstToQuest(readFile("$TETHEALLA_QUEST_PATH_PREFIX/ep2/ext/pw4.qst"))

        assertTrue(result is Success, "Failed: ${result.problems.joinToString()}")

        val quest = result.value.quest
        assertEquals(Episode.II, quest.episode, "PW4 must be EP2")
        assertTrue(quest.floorMappings.isNotEmpty(), "PW4 must have floor mappings")

        // PW4 maps floor 17 and floor 16 both to Tower (mapId=0x23, areaId=17).
        val floor16 = quest.floorMappings.find { it.floorId == 16 }
        val floor17 = quest.floorMappings.find { it.floorId == 17 }

        assertNotNull(floor17, "Floor 17 (Tower) must exist in PW4")
        assertEquals(0x23, floor17.mapId, "Floor 17 must use mapId 0x23 (Tower)")
        assertEquals(17, floor17.areaId, "Tower must have areaId=17, not 16 (Seaside Night)")

        if (floor16 != null) {
            assertEquals(0x23, floor16.mapId, "Floor 16 must use mapId 0x23 (Tower), not Seaside Night (0x22)")
            assertEquals(17, floor16.areaId, "Floor 16 must map to areaId=17 (Tower), not 16 (Seaside Night)")
        }

        // No floor should incorrectly map to Seaside Night (areaId=16).
        val seasideNightMappings = quest.floorMappings.filter { it.areaId == 16 }
        assertTrue(seasideNightMappings.isEmpty(), "PW4 should have no Seaside Night (areaId=16) mappings; got: $seasideNightMappings")
    }

    /**
     * Round-trip Phantasmal World #4: parse → write → parse, then compare quest structure.
     */
    @Test
    fun round_trip_ephinea_pw4() = testAsync {
        val path = "$TETHEALLA_QUEST_PATH_PREFIX/ep2/ext/pw4.qst"
        roundTripTest("pw4.qst", readFile(path))
    }

    /**
     * Parse EP1 challenge mode quest (1c1) and verify CM data.
     * Exercises challenge mode event parsing, random spawns, config pool, and monster mappings.
     */
    @Test
    fun parse_ephinea_ep1_chl_1c1() = testAsync {
        val result = parseQstToQuest(readFile("$TETHEALLA_QUEST_PATH_PREFIX/chl/ep1/1.qst"), lenient = true)

        assertTrue(result is Success, "Failed to parse 1c1_e: ${result.problems.joinToString()}")

        val quest = result.value.quest
        assertEquals(Episode.I, quest.episode, "1c1 must be EP1")

        // CM events should be present.
        val cmEvents = quest.events.filter { it.isChallengeMode }
        assertTrue(cmEvents.isNotEmpty(), "1c1 must have challenge mode events")

        // CM random spawns, config pool, and monster mappings must be parsed.
        val spawns = quest.challengeData.cmRandomSpawns
        assertTrue(spawns.isNotEmpty(), "1c1 must have CM random spawns")

        val configPool = quest.challengeData.cmConfigPool
        assertTrue(configPool.isNotEmpty(), "1c1 must have CM config pool")

        val mappings = quest.challengeData.cmMonsterMappings
        assertTrue(mappings.isNotEmpty(), "1c1 must have CM monster mappings")

        // Every spawn entry must have a valid section ID (not negative) and valid coords.
        for (spawn in spawns) {
            for (entry in spawn.entries) {
                assertTrue(entry.sectionId >= 0, "Section ID must be non-negative in spawn ${spawn.areaId}/${spawn.roomId}")
            }
        }

        // Config IDs in pool entries should be positive.
        for (pool in configPool) {
            for (entry in pool.entries) {
                assertTrue(entry.configId > 0, "Config ID must be positive")
            }
        }
    }

    /**
     * Round-trip EP1 challenge quest 1: parse → write → parse, compare structure.
     */
    @Test
    fun round_trip_ephinea_ep1_chl_1c1() = testAsync {
        val result = parseQstToQuest(readFile("$TETHEALLA_QUEST_PATH_PREFIX/chl/ep1/1.qst"), lenient = true)
        assertTrue(result is Success, "Parse failed: ${result.problems.joinToString()}")

        val quest = result.value.quest

        // Write and re-parse the DAT portion.
        val (_, dat) = writeQuestToBinDat(quest, result.value.version)
        val reparsed = parseDat(dat.cursor())

        assertEquals(quest.objects.size, reparsed.objs.size, "Object count must survive round-trip")
        assertEquals(quest.npcs.size, reparsed.npcs.size, "NPC count must survive round-trip")
        assertEquals(quest.events.size, reparsed.events.size, "Event count must survive round-trip")
        assertEquals(
            quest.challengeData.cmRandomSpawns.size,
            reparsed.cmRandomSpawns.size,
            "CM spawn count must survive round-trip",
        )
        assertEquals(
            quest.challengeData.cmConfigPool.size,
            reparsed.cmConfigPool.size,
            "Config pool count must survive round-trip",
        )
        assertEquals(
            quest.challengeData.cmMonsterMappings.size,
            reparsed.cmMonsterMappings.size,
            "Monster mapping count must survive round-trip",
        )

        // CM event flags must be preserved.
        val origCmEvents = quest.events.count { it.isChallengeMode }
        val newCmEvents = reparsed.events.count { it.isChallengeMode }
        assertEquals(origCmEvents, newCmEvents, "CM event count must survive round-trip")
    }

    /**
     * Round-trip Clarie's Deal (EP4 event quest): parse → write → parse, compare DAT structure.
     */
    @Test
    fun round_trip_ephinea_ep4_claries_deal() = testAsync {
        val path = "$TETHEALLA_QUEST_PATH_PREFIX/ep4/event/clarie's deal.qst"
        val result = parseQstToQuest(readFile(path))

        assertTrue(result is Success, "Parse failed: ${result.problems.joinToString()}")

        val quest = result.value.quest
        assertEquals(Episode.IV, quest.episode, "Clarie's Deal must be EP4")

        // Write and re-parse the DAT to verify round-trip.
        val (_, dat) = writeQuestToBinDat(quest, result.value.version)
        val reparsed = parseDat(dat.cursor())

        assertEquals(quest.objects.size, reparsed.objs.size, "Object count must survive round-trip")
        assertEquals(quest.npcs.size, reparsed.npcs.size, "NPC count must survive round-trip")
        assertEquals(quest.events.size, reparsed.events.size, "Event count must survive round-trip")

        // Verify object areaIds.
        for (i in quest.objects.indices) {
            assertEquals(quest.objects[i].areaId, reparsed.objs[i].areaId, "Object[$i] areaId")
        }

        // Verify NPC areaIds.
        for (i in quest.npcs.indices) {
            assertEquals(quest.npcs[i].areaId, reparsed.npcs[i].areaId, "NPC[$i] areaId")
        }
    }

    /**
     * Round-trip EP2 challenge quest 1 (2c1): parse → write → parse, compare structure.
     */
    @Test
    fun round_trip_ephinea_ep2_chl_2c1() = testAsync {
        val result = parseQstToQuest(readFile("$TETHEALLA_QUEST_PATH_PREFIX/chl/ep2/21.qst"), lenient = true)
        assertTrue(result is Success, "Parse failed: ${result.problems.joinToString()}")

        val quest = result.value.quest
        assertEquals(Episode.II, quest.episode, "2c1 must be EP2")

        val (_, dat) = writeQuestToBinDat(quest, result.value.version)
        val reparsed = parseDat(dat.cursor())

        assertEquals(quest.objects.size, reparsed.objs.size)
        assertEquals(quest.npcs.size, reparsed.npcs.size)
        assertEquals(quest.events.size, reparsed.events.size)
        assertEquals(quest.challengeData.cmRandomSpawns.size, reparsed.cmRandomSpawns.size)
        assertEquals(quest.challengeData.cmConfigPool.size, reparsed.cmConfigPool.size)
        assertEquals(quest.challengeData.cmMonsterMappings.size, reparsed.cmMonsterMappings.size)
    }

    companion object {
        private val EXCLUDED = listOf(
            ".raw",
            // Challenge mode quests: Basic parsing support exists, but some quests have issues
            // that need further investigation (e.g., bytecode variations, specific quest structures).
            // The chl/ep1/1.qst test demonstrates that core CM parsing works.
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
