package world.phantasmal.web.questEditor.controllers

import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.and
import world.phantasmal.observable.cell.eq
import world.phantasmal.observable.cell.list.ListCell
import world.phantasmal.observable.cell.list.listCell
import world.phantasmal.web.questEditor.actions.*
import world.phantasmal.web.questEditor.models.QuestEventActionModel
import world.phantasmal.web.questEditor.models.QuestEventModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.controllers.Controller

class EventsController(private val store: QuestEditorStore) : Controller() {
    val unavailable: Cell<Boolean> = store.currentQuest.isNull()
    val enabled: Cell<Boolean> = store.questEditingEnabled
    val removeEventEnabled: Cell<Boolean> = enabled and store.selectedEvent.isNotNull()
    val events: ListCell<QuestEventModel> = store.currentAreaEvents

    val eventActionTypes: ListCell<String> = listCell(
        QuestEventActionModel.SpawnNpcs.SHORT_NAME,
        QuestEventActionModel.Door.Unlock.SHORT_NAME,
        QuestEventActionModel.Door.Lock.SHORT_NAME,
        QuestEventActionModel.TriggerEvent.SHORT_NAME,
    )

    fun clicked() {
        selectEvent(null)
    }

    fun focused() {
        store.makeMainUndoCurrent()
    }

    fun isSelected(event: QuestEventModel): Cell<Boolean> =
        store.selectedEvent eq event

    fun selectEvent(event: QuestEventModel?) {
        store.setSelectedEvent(event)
    }

    fun addEvent() {
        val quest = store.currentQuest.value
        val area = store.currentArea.value

        if (quest != null && area != null) {
            val selectedEvent = store.selectedEvent.value
            val index =
                if (selectedEvent == null) quest.events.value.size
                else quest.events.value.indexOf(selectedEvent) + 1

            store.executeAction(
                CreateEventAction(
                    ::selectEvent,
                    quest,
                    index,
                    QuestEventModel(
                        id = 0,
                        areaId = area.id,
                        sectionId = 1,
                        waveId = 1,
                        delay = 0,
                        unknown = 0, // TODO: What's a sensible value for event.unknown?
                        actions = mutableListOf(),
                    ),
                )
            )
        }
    }

    fun removeSelectedEvent() {
        store.selectedEvent.value?.let(::removeEvent)
    }

    fun removeEvent(event: QuestEventModel) {
        val quest = store.currentQuest.value

        if (quest != null) {
            val index = quest.events.value.indexOf(event)

            if (index != -1) {
                store.executeAction(
                    DeleteEventAction(::selectEvent, quest, index, event)
                )
            }
        }
    }

    fun setId(event: QuestEventModel, id: Int) {
        store.executeAction(
            EditEventPropertyAction(
                "Edit ID of event ${event.id.value}",
                ::selectEvent,
                event,
                event::setId,
                id,
                event.id.value,
            )
        )
    }

    fun setSectionId(event: QuestEventModel, sectionId: Int) {
        store.executeAction(
            EditEventPropertyAction(
                "Edit section of event ${event.id.value}",
                ::selectEvent,
                event,
                event::setSectionId,
                sectionId,
                event.sectionId.value,
            )
        )
    }

    fun setWaveId(event: QuestEventModel, waveId: Int) {
        store.executeAction(
            EditEventPropertyAction(
                "Edit wave of event ${event.id}",
                ::selectEvent,
                event,
                event::setWaveId,
                waveId,
                event.wave.value.id,
            )
        )
    }

    fun setDelay(event: QuestEventModel, delay: Int) {
        store.executeAction(
            EditEventPropertyAction(
                "Edit delay of event ${event.id}",
                ::selectEvent,
                event,
                event::setDelay,
                delay,
                event.delay.value,
            )
        )
    }

    fun addAction(event: QuestEventModel, type: String) {
        val action = when (type) {
            QuestEventActionModel.SpawnNpcs.SHORT_NAME -> QuestEventActionModel.SpawnNpcs(0, 0)
            QuestEventActionModel.Door.Unlock.SHORT_NAME -> QuestEventActionModel.Door.Unlock(0)
            QuestEventActionModel.Door.Lock.SHORT_NAME -> QuestEventActionModel.Door.Lock(0)
            QuestEventActionModel.TriggerEvent.SHORT_NAME -> QuestEventActionModel.TriggerEvent(0)
            else -> error("""Unknown action type "$type".""")
        }

        store.executeAction(CreateEventActionAction(::selectEvent, event, action))
    }

    fun removeAction(event: QuestEventModel, action: QuestEventActionModel) {
        val index = event.actions.value.indexOf(action)
        store.executeAction(DeleteEventActionAction(::selectEvent, event, index, action))
    }

    fun canGoToEvent(eventId: Cell<Int>): Cell<Boolean> = store.canGoToEvent(eventId)

    fun goToEvent(eventId: Int) {
        store.goToEvent(eventId)
    }

    fun setActionSectionId(
        event: QuestEventModel,
        action: QuestEventActionModel.SpawnNpcs,
        sectionId: Int,
    ) {
        store.executeAction(
            EditEventPropertyAction(
                "Edit action section",
                ::selectEvent,
                event,
                action::setSectionId,
                sectionId,
                action.sectionId.value,
            )
        )
    }

    fun setActionAppearFlag(
        event: QuestEventModel,
        action: QuestEventActionModel.SpawnNpcs,
        appearFlag: Int,
    ) {
        store.executeAction(
            EditEventPropertyAction(
                "Edit action appear flag",
                ::selectEvent,
                event,
                action::setAppearFlag,
                appearFlag,
                action.appearFlag.value,
            )
        )
    }

    fun setActionDoorId(
        event: QuestEventModel,
        action: QuestEventActionModel.Door,
        doorId: Int,
    ) {
        store.executeAction(
            EditEventPropertyAction(
                "Edit action door",
                ::selectEvent,
                event,
                action::setDoorId,
                doorId,
                action.doorId.value,
            )
        )
    }

    fun setActionEventId(
        event: QuestEventModel,
        action: QuestEventActionModel.TriggerEvent,
        eventId: Int,
    ) {
        store.executeAction(
            EditEventPropertyAction(
                "Edit action event",
                ::selectEvent,
                event,
                action::setEventId,
                eventId,
                action.eventId.value,
            )
        )
    }
}
