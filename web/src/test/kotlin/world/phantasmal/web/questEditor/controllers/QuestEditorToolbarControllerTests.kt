package world.phantasmal.web.questEditor.controllers

import org.w3c.files.File
import world.phantasmal.core.Failure
import world.phantasmal.core.Severity
import world.phantasmal.lib.Episode
import world.phantasmal.lib.fileFormats.quest.NpcType
import world.phantasmal.web.core.actions.Action
import world.phantasmal.web.test.WebTestSuite
import world.phantasmal.web.test.createQuestModel
import world.phantasmal.web.test.createQuestNpcModel
import kotlin.test.*

class QuestEditorToolbarControllerTests : WebTestSuite() {
    @Test
    fun can_create_a_new_quest() = asyncTest {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.uiStore,
            components.questLoader,
            components.areaStore,
            components.questEditorStore,
        ))

        ctrl.createNewQuest(Episode.I)

        assertNotNull(components.questEditorStore.currentQuest.value)
    }

    @Test
    fun a_failure_is_exposed_when_openFiles_fails() = asyncTest {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.uiStore,
            components.questLoader,
            components.areaStore,
            components.questEditorStore,
        ))

        assertNull(ctrl.result.value)

        ctrl.openFiles(listOf(File(arrayOf(), "unknown.extension")))

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
    fun undo_state_changes_correctly() = asyncTest {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.uiStore,
            components.questLoader,
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

        // Add an action to the undo stack.
        components.questEditorStore.executeAction(
            object : Action {
                override val description: String = "Do action"
                override fun execute() {}
                override fun undo() {}
            }
        )

        assertEquals("Undo \"Do action\" (Ctrl-Z)", ctrl.undoTooltip.value)
        assertTrue(ctrl.undoEnabled.value)

        assertEquals(nothingToRedo, ctrl.redoTooltip.value)
        assertFalse(ctrl.redoEnabled.value)

        // Undo the previous action.
        ctrl.undo()

        assertEquals(nothingToUndo, ctrl.undoTooltip.value)
        assertFalse(ctrl.undoEnabled.value)

        assertEquals("Redo \"Do action\" (Ctrl-Shift-Z)", ctrl.redoTooltip.value)
        assertTrue(ctrl.redoEnabled.value)
    }

    @Test
    fun area_state_changes_correctly() = asyncTest {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.uiStore,
            components.questLoader,
            components.areaStore,
            components.questEditorStore,
        ))

        // No quest loaded.

        assertTrue(ctrl.areas.value.isEmpty())
        assertNull(ctrl.currentArea.value)
        assertFalse(ctrl.areaSelectEnabled.value)

        // Load quest.
        components.questEditorStore.setCurrentQuest(createQuestModel())

        assertTrue(ctrl.areas.value.isNotEmpty())
        assertNotNull(ctrl.currentArea.value)
        assertTrue(ctrl.areaSelectEnabled.value)
    }
}
