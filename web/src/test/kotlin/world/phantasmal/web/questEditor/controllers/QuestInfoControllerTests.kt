package world.phantasmal.web.questEditor.controllers

import world.phantasmal.lib.Episode
import world.phantasmal.web.test.WebTestSuite
import world.phantasmal.web.test.createQuestModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuestInfoControllerTests : WebTestSuite() {
    @Test
    fun exposes_correct_model_before_and_after_a_quest_is_loaded() = asyncTest {
        val store = components.questEditorStore
        val ctrl = disposer.add(QuestInfoController(store))

        assertTrue(ctrl.unavailable.value)
        assertFalse(ctrl.enabled.value)

        store.setCurrentQuest(createQuestModel(
            id = 25,
            name = "A Quest",
            shortDescription = "A short description.",
            longDescription = "A long description.",
            episode = Episode.II
        ))

        assertFalse(ctrl.unavailable.value)
        assertTrue(ctrl.enabled.value)
        assertEquals("II", ctrl.episode.value)
        assertEquals(25, ctrl.id.value)
        assertEquals("A Quest", ctrl.name.value)
        assertEquals("A short description.", ctrl.shortDescription.value)
        assertEquals("A long description.", ctrl.longDescription.value)
    }

    @Test
    fun can_edit_simple_properties_undo_edits_and_redo_edits() = asyncTest {
        val store = components.questEditorStore
        val ctrl = disposer.add(QuestInfoController(store))

        store.setCurrentQuest(createQuestModel(
            id = 1,
            name = "name 1",
            shortDescription = "short 1",
            longDescription = "long 1",
            episode = Episode.II
        ))

        assertTrue(ctrl.enabled.value)

        assertEquals(1, ctrl.id.value)
        assertEquals("name 1", ctrl.name.value)
        assertEquals("short 1", ctrl.shortDescription.value)
        assertEquals("long 1", ctrl.longDescription.value)

        ctrl.setId(2)
        ctrl.setName("name 2")
        ctrl.setShortDescription("short 2")
        ctrl.setLongDescription("long 2")

        assertEquals(2, ctrl.id.value)
        assertEquals("name 2", ctrl.name.value)
        assertEquals("short 2", ctrl.shortDescription.value)
        assertEquals("long 2", ctrl.longDescription.value)

        store.makeMainUndoCurrent()
        store.undo()
        store.undo()
        store.undo()
        store.undo()

        assertEquals(1, ctrl.id.value)
        assertEquals("name 1", ctrl.name.value)
        assertEquals("short 1", ctrl.shortDescription.value)
        assertEquals("long 1", ctrl.longDescription.value)

        store.redo()
        store.redo()
        store.redo()
        store.redo()

        assertEquals(2, ctrl.id.value)
        assertEquals("name 2", ctrl.name.value)
        assertEquals("short 2", ctrl.shortDescription.value)
        assertEquals("long 2", ctrl.longDescription.value)
    }

    @Test
    fun when_focused_main_undo_becomes_current_undo() = asyncTest {
        val store = components.questEditorStore
        val ctrl = disposer.add(QuestInfoController(store))

        // Put something on the undo stack.
        store.setCurrentQuest(createQuestModel(
            name = "original name",
        ))
        ctrl.setName("new name")

        components.undoManager.makeNopCurrent()

        // After focusing, the main undo stack becomes the current undo and we can undo.
        ctrl.focused()

        assertTrue(store.canUndo.value)
    }
}
