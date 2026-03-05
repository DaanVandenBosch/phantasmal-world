package world.phantasmal.web.questEditor.models

import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.asm.dataFlowAnalysis.FloorMapping
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
}
