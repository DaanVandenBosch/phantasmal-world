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
            components.questEditorUiStore,
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
            components.questEditorUiStore,
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
            components.questEditorUiStore,
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
            components.questEditorUiStore,
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
            components.questEditorUiStore,
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
            components.questEditorUiStore,
        ))

        assertNull(ctrl.result.value)

        ctrl.openFiles(listOf(FileHandle.Simple(File(arrayOf(), "unknown.extension"))))

        val result = ctrl.result.value

        assertTrue(result is Failure)
        assertEquals(1, result.problems.size)
        assertEquals(Severity.Error, result.problems.first().severity)
        assertEquals(
            "Please select a .qst file, a .bin file, or a .bin + .dat pair.",
            result.problems.first().uiMessage,
        )
    }

    @Test
    fun undo_state_changes_correctly() = testAsync {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.uiStore,
            components.areaStore,
            components.questEditorStore,
            components.questEditorUiStore,
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
            components.questEditorUiStore,
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
            components.questEditorUiStore,
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
        val ep2Areas = getAreasForEpisode(Episode.II)

        // Mapped areas: Lab (1 entry) + Tower (2 variant entries) = 3.
        val labEntries = areas.filter { it.area.id == 0 }
        assertEquals(1, labEntries.size, "Lab should have 1 entry")

        val towerEntries = areas.filter { it.area.id == 17 }
        assertEquals(2, towerEntries.size, "Tower should have 2 entries")
        assertNotNull(towerEntries[0].variant, "Tower entry should have a variant")
        assertNotNull(towerEntries[1].variant, "Tower entry should have a variant")
        assertNotEquals(
            towerEntries[0].variant!!.id,
            towerEntries[1].variant!!.id,
            "Tower entries should have different variant IDs",
        )

        // All unmapped episode areas should also be present (with no variant suffix).
        val mappedAreaIds = setOf(0, 17)
        val unmappedEp2Areas = ep2Areas.filter { it.id !in mappedAreaIds }
        for (unmappedArea in unmappedEp2Areas) {
            val entry = areas.find { it.area.id == unmappedArea.id }
            assertNotNull(entry, "Unmapped area '${unmappedArea.name}' (id=${unmappedArea.id}) should be in list")
            assertNull(entry.variant, "Unmapped area '${unmappedArea.name}' should have no variant")
            assertFalse(entry.label.contains("Map"), "Unmapped area '${unmappedArea.name}' should have no Map suffix")
        }

        // Total = 3 mapped entries + all unmapped areas.
        assertEquals(3 + unmappedEp2Areas.size, areas.size)

        // Areas should follow canonical episode order (by area id), not mapped-first.
        val areaIds = areas.map { it.area.id }
        val sortedAreaIds = areaIds.sorted()
        // Tower (id=17) has 2 entries, so allow consecutive duplicates but overall order must be non-decreasing.
        assertEquals(sortedAreaIds, areaIds, "Areas should be in canonical episode order")
    }

    @Test
    fun regular_quest_shows_all_episode_areas() = testAsync {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.uiStore,
            components.areaStore,
            components.questEditorStore,
            components.questEditorUiStore,
        ))

        // Regular quest with no floor mappings and only 1 NPC on area 1.
        val quest = createQuestModel(
            episode = Episode.I,
            npcs = listOf(createQuestNpcModel(NpcType.Boota, Episode.I, areaId = 1)),
        )
        components.questEditorStore.setCurrentQuest(quest)

        val areas = ctrl.areas.value
        val ep1Areas = getAreasForEpisode(Episode.I)

        // Every episode area should appear, even those with 0 entities.
        for (ep1Area in ep1Areas) {
            assertTrue(
                areas.any { it.area.id == ep1Area.id },
                "Area '${ep1Area.name}' (id=${ep1Area.id}) should be in list even with 0 entities",
            )
        }
    }

    @Test
    fun multi_floor_quest_shows_correct_entity_counts_per_variant() = testAsync {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.uiStore,
            components.areaStore,
            components.questEditorStore,
            components.questEditorUiStore,
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
            components.questEditorUiStore,
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

    // ---- Save As format selection tests ----

    @Test
    fun default_save_format_is_qst() = testAsync {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.uiStore,
            components.areaStore,
            components.questEditorStore,
            components.questEditorUiStore,
        ))

        assertEquals(SaveFormat.QST, ctrl.saveFormat.value)
    }

    @Test
    fun compressed_visible_only_for_bin_dat() = testAsync {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.uiStore,
            components.areaStore,
            components.questEditorStore,
            components.questEditorUiStore,
        ))

        // Default is QST — compressed checkbox hidden.
        ctrl.setSaveFormat(SaveFormat.QST)
        assertFalse(ctrl.compressedVisible.value, "Compressed should be hidden for QST")

        // BIN_DAT — compressed checkbox visible.
        ctrl.setSaveFormat(SaveFormat.BIN_DAT)
        assertTrue(ctrl.compressedVisible.value, "Compressed should be visible for BIN_DAT")
    }

    @Test
    fun available_formats_always_include_qst_and_bin_dat() = testAsync {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.uiStore,
            components.areaStore,
            components.questEditorStore,
            components.questEditorUiStore,
        ))

        assertTrue(ctrl.availableSaveFormats.contains(SaveFormat.QST))
        assertTrue(ctrl.availableSaveFormats.contains(SaveFormat.BIN_DAT))
    }

    @Test
    fun set_save_format_updates_cell() = testAsync {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.uiStore,
            components.areaStore,
            components.questEditorStore,
            components.questEditorUiStore,
        ))

        ctrl.setSaveFormat(SaveFormat.BIN_DAT)
        assertEquals(SaveFormat.BIN_DAT, ctrl.saveFormat.value)

        ctrl.setSaveFormat(SaveFormat.QST)
        assertEquals(SaveFormat.QST, ctrl.saveFormat.value)
    }
}
