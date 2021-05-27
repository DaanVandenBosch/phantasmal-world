package world.phantasmal.web.questEditor.controllers

import world.phantasmal.web.questEditor.models.QuestEventActionModel
import world.phantasmal.web.test.WebTestSuite
import world.phantasmal.web.test.createQuestModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventsControllerTests : WebTestSuite {
    @Test
    fun addEvent() = testAsync {
        // Setup.
        val store = components.questEditorStore
        val quest = createQuestModel(mapDesignations = mapOf(1 to 0))
        store.setCurrentQuest(quest)
        store.setCurrentArea(quest.areaVariants.value.first().area)
        store.makeMainUndoCurrent()

        val ctrl = disposer.add(EventsController(store))

        // Add an event.
        ctrl.addEvent()

        assertEquals(1, quest.events.value.size)

        // Undo.
        assertTrue(store.canUndo.value)
        assertFalse(store.canRedo.value)

        store.undo()

        assertTrue(quest.events.value.isEmpty())

        // Redo.
        assertFalse(store.canUndo.value)
        assertTrue(store.canRedo.value)

        store.redo()

        assertEquals(1, quest.events.value.size)
    }

    @Test
    fun addAction() = testAsync {
        // Setup.
        val store = components.questEditorStore
        val quest = createQuestModel(mapDesignations = mapOf(1 to 0))
        store.setCurrentQuest(quest)
        store.setCurrentArea(quest.areaVariants.value.first().area)
        store.makeMainUndoCurrent()

        val ctrl = disposer.add(EventsController(store))

        // Add an event and an action.
        ctrl.addEvent()
        val event = ctrl.events.value.first()
        ctrl.addAction(event, QuestEventActionModel.Door.Unlock.SHORT_NAME)

        // Undo.
        assertTrue(store.canUndo.value)
        assertFalse(store.canRedo.value)

        store.undo()

        assertTrue(event.actions.value.isEmpty())

        // Redo.
        assertTrue(store.canUndo.value) // Can still undo event creation at this point.
        assertTrue(store.canRedo.value)

        store.redo()

        assertEquals(1, event.actions.value.size)
    }
}
