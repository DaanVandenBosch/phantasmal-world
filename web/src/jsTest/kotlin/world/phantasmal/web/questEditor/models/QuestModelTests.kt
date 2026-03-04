package world.phantasmal.web.questEditor.models

import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.asm.BytecodeIr
import world.phantasmal.psolib.asm.dataFlowAnalysis.FloorMapping
import world.phantasmal.web.test.WebTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class QuestModelTests : WebTestSuite {

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
            mapDesignations = emptyMap(),
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
            mapDesignations = emptyMap(),
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
