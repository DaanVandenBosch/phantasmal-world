package world.phantasmal.web.questEditor.models

import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.asm.BytecodeIr
import world.phantasmal.psolib.asm.assemble
import world.phantasmal.psolib.asm.dataFlowAnalysis.FloorMapping
import world.phantasmal.psolib.asm.dataFlowAnalysis.ParticleSpawnSource
import world.phantasmal.core.Success
import world.phantasmal.psolib.fileFormats.quest.NpcType
import world.phantasmal.psolib.fileFormats.quest.ObjectType
import world.phantasmal.web.test.WebTestSuite
import world.phantasmal.web.test.createQuestModel
import world.phantasmal.web.test.createQuestNpcModel
import world.phantasmal.web.test.createQuestObjectModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class QuestModelTests : WebTestSuite {
    @Test
    fun particle_spawns_follow_dat_object_edits_and_list_changes() = test {
        val particleObject = createQuestObjectModel(ObjectType.Particle, areaId = 2)
        val particleType = particleObject.properties.value.single { it.offset == 40 }
        particleType.setValue(44.9f)
        val quest = createQuestModel(objects = listOf(particleObject))

        assertEquals(44, quest.particleSpawns.value.single().particleId)

        particleType.setValue(45.9f)
        assertEquals(45, quest.particleSpawns.value.single().particleId)

        quest.removeEntity(particleObject)
        assertTrue(quest.particleSpawns.value.isEmpty())

        quest.addObject(particleObject)
        assertEquals(45, quest.particleSpawns.value.single().particleId)
    }

    @Test
    fun opcode_particle_spawns_follow_bytecode_edits() = test {
        fun bytecode(particleId: Int): BytecodeIr {
            val result = assemble(listOf(
                "0:",
                "leti r0, 10",
                "leti r1, 20",
                "leti r2, 30",
                "leti r3, $particleId",
                "leti r4, 60",
                "particle_v3 r0",
                "ret",
            ))
            assertTrue(result is Success)
            return result.value
        }

        val quest = createQuestModel(bytecodeIr = bytecode(41))
        assertEquals(41, quest.particleSpawns.value.single().particleId)

        quest.setBytecodeIr(bytecode(42))

        val spawn = quest.particleSpawns.value.single()
        assertEquals(42, spawn.particleId)
        assertTrue(spawn.source is ParticleSpawnSource.Opcode)
    }

    @Test
    fun entitiesPerArea_maps_floor_ids_to_area_ids() = test {
        // EP2: floor 17 and floor 16 both map to area 17 (Tower)
        val quest = createQuestModel(
            episode = Episode.II,
            floorMappings = listOf(
                FloorMapping(floorId = 0, mapId = 0x12, areaId = 0, variantId = 0),
                FloorMapping(floorId = 17, mapId = 0x23, areaId = 17, variantId = 0),
                FloorMapping(floorId = 16, mapId = 0x23, areaId = 17, variantId = 1),
            ),
            npcs = listOf(
                createQuestNpcModel(NpcType.Boota, Episode.II, areaId = 17), // floor 17
                createQuestNpcModel(NpcType.Boota, Episode.II, areaId = 16), // floor 16
            ),
            objects = listOf(
                createQuestObjectModel(ObjectType.PlayerSet, areaId = 0),    // floor 0
            ),
        )

        val entitiesPerArea = quest.entitiesPerArea.value

        // Both NPCs on floor 16 and 17 should count toward area 17 (Tower)
        assertEquals(2, entitiesPerArea[17], "Area 17 (Tower) should have 2 entities")
        // Object on floor 0 should count toward area 0 (Lab)
        assertEquals(1, entitiesPerArea[0], "Area 0 (Lab) should have 1 entity")
        // Floor IDs 16 and 17 should NOT appear as keys (they should be mapped to area 17)
        assertTrue(16 !in entitiesPerArea, "Floor ID 16 should not be a key in entitiesPerArea")
    }

    @Test
    fun floorToVariantMap_maps_floor_ids_to_variants() = test {
        val quest = createQuestModel(
            episode = Episode.II,
            floorMappings = listOf(
                FloorMapping(floorId = 0, mapId = 0x12, areaId = 0, variantId = 0),
                FloorMapping(floorId = 17, mapId = 0x23, areaId = 17, variantId = 0),
                FloorMapping(floorId = 16, mapId = 0x23, areaId = 17, variantId = 1),
            ),
        )

        val map = quest.floorToVariantMap

        assertEquals(3, map.size)

        // Floor 0 -> Lab variant 0
        val floor0Variant = map[0]
        assertNotNull(floor0Variant)
        assertEquals(0, floor0Variant.area.id)
        assertEquals(0, floor0Variant.id)

        // Floor 17 -> Tower variant 0
        val floor17Variant = map[17]
        assertNotNull(floor17Variant)
        assertEquals(17, floor17Variant.area.id)
        assertEquals(0, floor17Variant.id)

        // Floor 16 -> Tower variant 1
        val floor16Variant = map[16]
        assertNotNull(floor16Variant)
        assertEquals(17, floor16Variant.area.id)
        assertEquals(1, floor16Variant.id)
    }

    @Test
    fun floorToVariantMap_is_empty_for_regular_quests() = test {
        val quest = createQuestModel(episode = Episode.I)

        assertTrue(quest.floorToVariantMap.isEmpty())
    }

    @Test
    fun areaVariants_contains_unique_variants_for_multi_floor() = test {
        val quest = createQuestModel(
            episode = Episode.II,
            floorMappings = listOf(
                FloorMapping(floorId = 0, mapId = 0x12, areaId = 0, variantId = 0),
                FloorMapping(floorId = 17, mapId = 0x23, areaId = 17, variantId = 0),
                FloorMapping(floorId = 16, mapId = 0x23, areaId = 17, variantId = 1),
            ),
        )

        val variants = quest.areaVariants.value

        // Should have 3 variants: Lab v0, Tower v0, Tower v1
        assertEquals(3, variants.size)

        val towerVariants = variants.filter { it.area.id == 17 }
        assertEquals(2, towerVariants.size, "Tower should have 2 variants")
        assertEquals(
            setOf(0, 1),
            towerVariants.map { it.id }.toSet(),
            "Tower variants should be 0 and 1",
        )

        val labVariants = variants.filter { it.area.id == 0 }
        assertEquals(1, labVariants.size, "Lab should have 1 variant")
    }

    /**
     * When an EP4 quest uses bb_map_designate to reference an EP2 map (e.g., mapId 0x12 = Lab),
     * the QuestModel should resolve the variant using EP2's area list, not EP4's.
     *
     * Without the fix, areaId=0 with Episode.IV resolves to Pioneer II (EP4's area 0).
     * With the fix, mapEpisode=Episode.II is used, so areaId=0 resolves to Lab (EP2's area 0).
     */
    @Test
    fun cross_episode_floor_mapping_resolves_correct_variant() = test {
        // Simulate what "Lost SON HOPKINS" quest does:
        // EP4 quest with bb_map_designate 0, 18, 0, 0 (floor 0 -> EP2 Lab mapId=0x12)
        val floorMappings = listOf(
            FloorMapping(floorId = 0, mapId = 0x12, areaId = 0, variantId = 0, mapEpisode = Episode.II),
            FloorMapping(floorId = 6, mapId = 0x29, areaId = 6, variantId = 0, mapEpisode = Episode.IV),
            FloorMapping(floorId = 7, mapId = 0x2A, areaId = 7, variantId = 0, mapEpisode = Episode.IV),
        )

        val quest = QuestModel(
            id = 1,
            language = 1,
            name = "Test",
            shortDescription = "Test",
            longDescription = "Test",
            episode = Episode.IV,
            npcs = mutableListOf(),
            objects = mutableListOf(),
            events = mutableListOf(),
            datUnknowns = emptyList(),
            cmRandomSpawns = mutableListOf(),
            cmMonsterMappings = mutableListOf(),
            cmConfigPool = mutableListOf(),
            bytecodeIr = BytecodeIr(emptyList()),
            shopItems = UIntArray(0),
            floorMappings = floorMappings,
            getVariant = components.areaStore::getVariant,
        )

        // Verify floor 0 maps to Lab (EP2), not Pioneer II (EP4)
        val floor0Variant = quest.floorToVariantMap[0]
        assertNotNull(floor0Variant, "Floor 0 should have a variant mapping")
        assertEquals("Lab", floor0Variant.area.name,
            "Floor 0 should be Lab (EP2), not Pioneer II (EP4)")
        assertEquals(Episode.II, floor0Variant.episode,
            "Floor 0 variant should belong to Episode.II")

        // Verify EP4 floors still resolve correctly
        val floor6Variant = quest.floorToVariantMap[6]
        assertNotNull(floor6Variant, "Floor 6 should have a variant mapping")
        assertEquals("Subterranean Desert 1", floor6Variant.area.name)
        assertEquals(Episode.IV, floor6Variant.episode)

        // Verify areaVariants contains Lab, not Pioneer II for floor 0
        val labVariant = quest.areaVariants.value.find { it.area.name == "Lab" }
        assertNotNull(labVariant, "areaVariants should contain Lab variant")

        val pioneerVariant = quest.areaVariants.value.find { it.area.name == "Pioneer II" }
        assertTrue(pioneerVariant == null,
            "areaVariants should NOT contain Pioneer II (EP4 area 0)")
    }

    /**
     * When floor mappings use maps from the same episode, mapEpisode should match
     * the quest episode and behavior should be unchanged.
     */
    @Test
    fun same_episode_floor_mapping_resolves_correctly() = test {
        val floorMappings = listOf(
            FloorMapping(floorId = 0, mapId = 0x2D, areaId = 0, variantId = 0, mapEpisode = Episode.IV),
            FloorMapping(floorId = 1, mapId = 0x24, areaId = 1, variantId = 0, mapEpisode = Episode.IV),
        )

        val quest = QuestModel(
            id = 2,
            language = 1,
            name = "Test EP4",
            shortDescription = "Test",
            longDescription = "Test",
            episode = Episode.IV,
            npcs = mutableListOf(),
            objects = mutableListOf(),
            events = mutableListOf(),
            datUnknowns = emptyList(),
            cmRandomSpawns = mutableListOf(),
            cmMonsterMappings = mutableListOf(),
            cmConfigPool = mutableListOf(),
            bytecodeIr = BytecodeIr(emptyList()),
            shopItems = UIntArray(0),
            floorMappings = floorMappings,
            getVariant = components.areaStore::getVariant,
        )

        // Floor 0 should be Pioneer II (EP4's area 0)
        val floor0Variant = quest.floorToVariantMap[0]
        assertNotNull(floor0Variant)
        assertEquals("Pioneer II", floor0Variant.area.name)
        assertEquals(Episode.IV, floor0Variant.episode)

        // Floor 1 should be Crater Route 1
        val floor1Variant = quest.floorToVariantMap[1]
        assertNotNull(floor1Variant)
        assertEquals("Crater Route 1", floor1Variant.area.name)
        assertEquals(Episode.IV, floor1Variant.episode)
    }
}
