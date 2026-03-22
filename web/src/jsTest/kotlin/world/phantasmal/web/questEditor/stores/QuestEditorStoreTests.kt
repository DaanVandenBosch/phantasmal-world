package world.phantasmal.web.questEditor.stores

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

class QuestEditorStoreTests : WebTestSuite {
    @Test
    fun setCurrentQuest_selects_floor_0_area_for_multi_floor() = testAsync {
        val store = components.questEditorStore

        val quest = createQuestModel(
            episode = Episode.II,
            floorMappings = listOf(
                FloorMapping(floorId = 0, mapId = 0x12, areaId = 0, variantId = 0),
                FloorMapping(floorId = 17, mapId = 0x23, areaId = 17, variantId = 0),
                FloorMapping(floorId = 16, mapId = 0x23, areaId = 17, variantId = 1),
            ),
        )
        store.setCurrentQuest(quest)

        // Should select area 0 (Lab) as the initial area (floor 0 mapping)
        assertNotNull(store.currentArea.value)
        assertEquals(0, store.currentArea.value?.id, "Initial area should be Lab (area 0)")

        // Should select variant 0 for Lab
        assertNotNull(store.currentAreaVariant.value)
        assertEquals(0, store.currentAreaVariant.value?.id, "Initial variant should be 0")
        assertEquals(0, store.currentAreaVariant.value?.area?.id)
    }

    @Test
    fun setSelectedEntity_switches_to_correct_area_and_variant() = testAsync {
        val store = components.questEditorStore

        // NPC on floor 16 (Tower variant 1)
        val npc = createQuestNpcModel(NpcType.Boota, Episode.II, areaId = 16)
        val quest = createQuestModel(
            episode = Episode.II,
            floorMappings = listOf(
                FloorMapping(floorId = 0, mapId = 0x12, areaId = 0, variantId = 0),
                FloorMapping(floorId = 17, mapId = 0x23, areaId = 17, variantId = 0),
                FloorMapping(floorId = 16, mapId = 0x23, areaId = 17, variantId = 1),
            ),
            npcs = listOf(npc),
        )
        store.setCurrentQuest(quest)

        // Initially on Lab (floor 0)
        assertEquals(0, store.currentArea.value?.id)

        // Select the NPC on floor 16 -> should switch to Tower (area 17, variant 1)
        store.setSelectedEntity(npc)

        assertEquals(17, store.currentArea.value?.id, "Should switch to Tower (area 17)")
        assertEquals(1, store.currentAreaVariant.value?.id, "Should switch to variant 1")
    }

    @Test
    fun convertQuestFromModel_preserves_floorMappings() = testAsync {
        val floorMappings = listOf(
            FloorMapping(floorId = 0, mapId = 0x12, areaId = 0, variantId = 0),
            FloorMapping(floorId = 17, mapId = 0x23, areaId = 17, variantId = 0),
            FloorMapping(floorId = 16, mapId = 0x23, areaId = 17, variantId = 1),
        )
        val model = createQuestModel(
            episode = Episode.II,
            floorMappings = floorMappings,
            npcs = listOf(createQuestNpcModel(NpcType.Boota, Episode.II, areaId = 17)),
            objects = listOf(createQuestObjectModel(ObjectType.PlayerSet, areaId = 0)),
        )

        val quest = convertQuestFromModel(model)

        assertEquals(floorMappings.size, quest.floorMappings.size)
        for (i in floorMappings.indices) {
            assertEquals(floorMappings[i], quest.floorMappings[i])
        }
    }
}
