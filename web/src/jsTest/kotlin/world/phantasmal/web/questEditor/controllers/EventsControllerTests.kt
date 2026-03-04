package world.phantasmal.web.questEditor.controllers

import world.phantasmal.web.questEditor.models.QuestEventActionModel
import world.phantasmal.web.questEditor.models.QuestEventModel
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
        val quest = createQuestModel(mapDesignations = mapOf(1 to setOf(0)))
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
        assertTrue(store.canUndo.value)
        assertFalse(store.canRedo.value)
    }

    @Test
    fun addAction() = testAsync {
        // Setup.
        val store = components.questEditorStore
        val quest = createQuestModel(mapDesignations = mapOf(1 to setOf(0)))
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

    @Test
    fun canGoToEvent() = testAsync {
        // Setup.
        val store = components.questEditorStore
        // Quest with two events, the first event triggers the second event.
        val quest = createQuestModel(
            mapDesignations = mapOf(1 to setOf(0)),
            events = listOf(
                QuestEventModel(
                    id = 100,
                    areaId = 1,
                    sectionId = 11,
                    waveId = 1,
                    delay = 50,
                    unknown = 0,
                    actions = mutableListOf(QuestEventActionModel.TriggerEvent(101)),
                ),
                QuestEventModel(
                    id = 101,
                    areaId = 1,
                    sectionId = 11,
                    waveId = 2,
                    delay = 50,
                    unknown = 0,
                    actions = mutableListOf(QuestEventActionModel.Door.Unlock(7)),
                ),
            ),
        )
        store.setCurrentQuest(quest)
        store.setCurrentArea(quest.areaVariants.value.first().area)

        val ctrl = disposer.add(EventsController(store))

        val canGoToEvent = ctrl.canGoToEvent(
            (ctrl.events[0].actions[0] as QuestEventActionModel.TriggerEvent).eventId
        )

        assertEquals(true, canGoToEvent.value)

        // Let event 100 point to nonexistent event 102.
        ctrl.setActionEventId(
            ctrl.events[0],
            ctrl.events[0].actions[0] as QuestEventActionModel.TriggerEvent,
            102,
        )

        assertEquals(false, canGoToEvent.value)

        // Add event 102.
        ctrl.selectEvent(null) // Deselect so the next event will be added at the end of the list.
        ctrl.addEvent()
        ctrl.setId(ctrl.events.value.last(), 102)

        assertEquals(true, canGoToEvent.value)

        // Remove event 102.
        ctrl.removeEvent(ctrl.events.value.last())

        assertEquals(false, canGoToEvent.value)
    }
}
