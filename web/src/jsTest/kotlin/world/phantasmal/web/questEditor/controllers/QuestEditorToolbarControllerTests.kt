package world.phantasmal.web.questEditor.controllers

import org.w3c.files.File
import world.phantasmal.core.Failure
import world.phantasmal.core.Severity
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.asm.dataFlowAnalysis.FloorMapping
import world.phantasmal.psolib.fileFormats.quest.NpcType
import world.phantasmal.psolib.fileFormats.quest.ObjectType
import world.phantasmal.psolib.fileFormats.quest.getAreasForEpisode
import world.phantasmal.web.core.commands.Command
import world.phantasmal.web.test.WebTestSuite
import world.phantasmal.web.test.createQuestModel
import world.phantasmal.web.test.createQuestNpcModel
import world.phantasmal.web.test.createQuestObjectModel
import world.phantasmal.webui.files.FileHandle
import kotlin.test.*

class QuestEditorToolbarControllerTests : WebTestSuite {
    @Test
    fun can_create_a_new_quest() = testAsync {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.uiStore,
            components.areaStore,
            components.questEditorStore,
        ))

        ctrl.createNewQuest(Episode.I)

        assertNotNull(components.questEditorStore.currentQuest.value)
    }

    @Test
    fun can_load_city_quest() = testAsync {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.uiStore,
            components.areaStore,
            components.questEditorStore,
        ))

        ctrl.loadCityQuest(Episode.I)

        val quest = components.questEditorStore.currentQuest.value
        assertNotNull(quest)
        assertEquals(Episode.I, quest.episode)
    }

    @Test
    fun can_load_lobby_quest() = testAsync {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.uiStore,
            components.areaStore,
            components.questEditorStore,
        ))

        ctrl.loadLobbyQuest(1)

        val quest = components.questEditorStore.currentQuest.value
        assertNotNull(quest)
        assertEquals(Episode.I, quest.episode)
    }

    @Test
    fun city_map_toggle_switches_quest_type() = testAsync {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.uiStore,
            components.areaStore,
            components.questEditorStore,
        ))

        // Start with default quest.
        ctrl.createNewQuest(Episode.I)
        assertFalse(ctrl.showCityMap.value)

        // Toggle city map on.
        ctrl.setShowCityMap(true)
        assertTrue(ctrl.showCityMap.value)
        assertNotNull(components.questEditorStore.currentQuest.value)

        // Toggle city map off.
        ctrl.setShowCityMap(false)
        assertFalse(ctrl.showCityMap.value)
        assertNotNull(components.questEditorStore.currentQuest.value)
    }

    @Test
    fun city_map_state_preserved_across_episode_switch() = testAsync {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.uiStore,
            components.areaStore,
            components.questEditorStore,
        ))

        // Enable city map, then switch episode.
        ctrl.loadCityQuest(Episode.I)
        assertTrue(ctrl.showCityMap.value)

        ctrl.createNewQuest(Episode.II)
        assertTrue(ctrl.showCityMap.value, "City map state should be preserved")
        assertEquals(Episode.II, components.questEditorStore.currentQuest.value?.episode)
    }

    @Test
    fun a_failure_is_exposed_when_openFiles_fails() = testAsync {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.uiStore,
            components.areaStore,
            components.questEditorStore,
        ))

        assertNull(ctrl.result.value)

        ctrl.openFiles(listOf(FileHandle.Simple(File(arrayOf(), "unknown.extension"))))

        val result = ctrl.result.value

        assertTrue(result is Failure)
        assertEquals(1, result.problems.size)
        assertEquals(Severity.Error, result.problems.first().severity)
        assertEquals(
            "Please select a .qst file or one .bin and one .dat file.",
            result.problems.first().uiMessage,
        )
    }

    @Test
    fun undo_state_changes_correctly() = testAsync {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.uiStore,
            components.areaStore,
            components.questEditorStore,
        ))
        components.questEditorStore.makeMainUndoCurrent()
        val nothingToUndo = "Nothing to undo (Ctrl-Z)"
        val nothingToRedo = "Nothing to redo (Ctrl-Shift-Z)"

        // No quest loaded.

        assertEquals(nothingToUndo, ctrl.undoTooltip.value)
        assertFalse(ctrl.undoEnabled.value)

        assertEquals(nothingToRedo, ctrl.redoTooltip.value)
        assertFalse(ctrl.redoEnabled.value)

        // Load quest.
        val npc = createQuestNpcModel(NpcType.Scientist, Episode.I)
        val quest = createQuestModel(name = "Old Name", npcs = listOf(npc))
        components.questEditorStore.setCurrentQuest(quest)

        assertEquals(nothingToUndo, ctrl.undoTooltip.value)
        assertFalse(ctrl.undoEnabled.value)

        assertEquals(nothingToRedo, ctrl.redoTooltip.value)
        assertFalse(ctrl.redoEnabled.value)

        // Add a command to the undo stack.
        components.questEditorStore.executeAction(
            object : Command {
                override val description: String = "Do command"
                override fun execute() {}
                override fun undo() {}
            }
        )

        assertEquals("Undo \"Do command\" (Ctrl-Z)", ctrl.undoTooltip.value)
        assertTrue(ctrl.undoEnabled.value)

        assertEquals(nothingToRedo, ctrl.redoTooltip.value)
        assertFalse(ctrl.redoEnabled.value)

        // Undo the previous command.
        ctrl.undo()

        assertEquals(nothingToUndo, ctrl.undoTooltip.value)
        assertFalse(ctrl.undoEnabled.value)

        assertEquals("Redo \"Do command\" (Ctrl-Shift-Z)", ctrl.redoTooltip.value)
        assertTrue(ctrl.redoEnabled.value)
    }

    @Test
    fun state_changes_correctly_when_a_quest_is_loaded() = testAsync {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.uiStore,
            components.areaStore,
            components.questEditorStore,
        ))

        // No quest loaded.

        // No current area and no areas to select.
        assertTrue(ctrl.areas.value.isEmpty())
        assertNull(ctrl.currentArea.value)
        assertFalse(ctrl.areaSelectEnabled.value)
        // Nothing to save.
        assertFalse(ctrl.saveAsEnabled.value)

        // Load quest.
        components.questEditorStore.setCurrentQuest(createQuestModel())

        // We have some areas and one area is selected at this point.
        assertTrue(ctrl.areas.value.isNotEmpty())
        assertNotNull(ctrl.currentArea.value)
        assertTrue(ctrl.areaSelectEnabled.value)
        // We can save the current quest.
        assertTrue(ctrl.saveAsEnabled.value)
    }

    @Test
    fun multi_floor_quest_shows_all_episode_areas() = testAsync {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.uiStore,
            components.areaStore,
            components.questEditorStore,
        ))

        // Simulate PW4-like quest: EP2 with Lab (floor 0) and two Tower variants (floors 16, 17).
        // EP2 area 0 = Lab (mapId=0x12), area 17 = Tower (mapId=0x23)
        val quest = createQuestModel(
            episode = Episode.II,
            floorMappings = listOf(
                FloorMapping(floorId = 0, mapId = 0x12, areaId = 0, variantId = 0),
                FloorMapping(floorId = 17, mapId = 0x23, areaId = 17, variantId = 0),
                FloorMapping(floorId = 16, mapId = 0x23, areaId = 17, variantId = 1),
            ),
        )
        components.questEditorStore.setCurrentQuest(quest)

        val areas = ctrl.areas.value
        val allEp2Areas = getAreasForEpisode(Episode.II)

        // Should have all EP2 areas, plus one extra entry for Tower's second variant.
        // Tower (area 17) appears twice (variant 0 and variant 1), all other areas appear once.
        assertEquals(allEp2Areas.size + 1, areas.size)

        // Tower entries should have variants
        val towerEntries = areas.filter { it.area.id == 17 }
        assertEquals(2, towerEntries.size, "Tower should have 2 entries")
        assertNotNull(towerEntries[0].variant, "Tower entry should have a variant")
        assertNotNull(towerEntries[1].variant, "Tower entry should have a variant")
        assertNotEquals(
            towerEntries[0].variant!!.id,
            towerEntries[1].variant!!.id,
            "Tower entries should have different variant IDs",
        )

        // Non-floor-mapped areas should still appear (without variant)
        val vrTempleAlpha = areas.find { it.area.id == 1 }
        assertNotNull(vrTempleAlpha, "VR Temple Alpha should be in area list")
        assertNull(vrTempleAlpha.variant, "Non-floor-mapped area should have no variant")
    }

    @Test
    fun multi_floor_quest_shows_correct_entity_counts_per_variant() = testAsync {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.uiStore,
            components.areaStore,
            components.questEditorStore,
        ))

        // EP2: Lab (floor 0), Tower v0 (floor 17), Tower v1 (floor 16)
        // Put 2 NPCs on floor 17 (Tower v0) and 1 object on floor 16 (Tower v1)
        val quest = createQuestModel(
            episode = Episode.II,
            floorMappings = listOf(
                FloorMapping(floorId = 0, mapId = 0x12, areaId = 0, variantId = 0),
                FloorMapping(floorId = 17, mapId = 0x23, areaId = 17, variantId = 0),
                FloorMapping(floorId = 16, mapId = 0x23, areaId = 17, variantId = 1),
            ),
            npcs = listOf(
                createQuestNpcModel(NpcType.Boota, Episode.II, areaId = 17),
                createQuestNpcModel(NpcType.Boota, Episode.II, areaId = 17),
            ),
            objects = listOf(
                createQuestObjectModel(ObjectType.PlayerSet, areaId = 16),
            ),
        )
        components.questEditorStore.setCurrentQuest(quest)

        val areas = ctrl.areas.value
        val towerEntries = areas.filter { it.area.id == 17 }
        assertEquals(2, towerEntries.size)

        // Tower v0 (floor 17) should show 2 entities
        val towerV0 = towerEntries.find { it.variant?.id == 0 }
        assertNotNull(towerV0)
        assertTrue(towerV0.label.contains("(2)"), "Tower v0 should show 2 entities, got: ${towerV0.label}")

        // Tower v1 (floor 16) should show 1 entity
        val towerV1 = towerEntries.find { it.variant?.id == 1 }
        assertNotNull(towerV1)
        assertTrue(towerV1.label.contains("(1)"), "Tower v1 should show 1 entity, got: ${towerV1.label}")

        // Lab (floor 0) should show no entity count
        val lab = areas.find { it.area.id == 0 }
        assertNotNull(lab)
        assertFalse(lab.label.contains("("), "Lab should have no entity count, got: ${lab.label}")
    }

    @Test
    fun multi_floor_quest_area_selection_sets_correct_variant() = testAsync {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.uiStore,
            components.areaStore,
            components.questEditorStore,
        ))
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

        val areas = ctrl.areas.value
        val towerEntries = areas.filter { it.area.id == 17 }

        // Select Tower variant 1
        val towerV1 = towerEntries.find { it.variant?.id == 1 }!!
        ctrl.setCurrentArea(towerV1)

        assertEquals(17, store.currentArea.value?.id)
        assertEquals(1, store.currentAreaVariant.value?.id)

        // Select Tower variant 0
        val towerV0 = towerEntries.find { it.variant?.id == 0 }!!
        ctrl.setCurrentArea(towerV0)

        assertEquals(17, store.currentArea.value?.id)
        assertEquals(0, store.currentAreaVariant.value?.id)

        // currentArea in controller should match
        assertNotNull(ctrl.currentArea.value)
        assertEquals(0, ctrl.currentArea.value?.variant?.id)
    }
}
