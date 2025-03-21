package world.phantasmal.web.questEditor.controllers

import org.w3c.files.File
import world.phantasmal.core.Failure
import world.phantasmal.core.Severity
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.fileFormats.quest.NpcType
import world.phantasmal.web.core.commands.Command
import world.phantasmal.web.test.WebTestSuite
import world.phantasmal.web.test.createQuestModel
import world.phantasmal.web.test.createQuestNpcModel
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
}
