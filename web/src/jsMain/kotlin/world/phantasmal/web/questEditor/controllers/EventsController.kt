package world.phantasmal.web.questEditor.controllers

import world.phantasmal.cell.*
import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.list.listCell
import world.phantasmal.web.questEditor.commands.*
import world.phantasmal.web.questEditor.models.QuestEventActionModel
import world.phantasmal.web.questEditor.models.QuestEventModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.controllers.Controller

class EventsController(private val store: QuestEditorStore) : Controller() {
    val unavailable: Cell<Boolean> = store.currentQuest.isNull()
    val enabled: Cell<Boolean> = store.questEditingEnabled
    val removeEventEnabled: Cell<Boolean> = enabled and store.selectedEvent.isNotNull()
    val events: ListCell<QuestEventModel> = store.currentAreaEvents

    // Track current area and variant for scroll reset on floor changes
    val currentAreaIdentifier: Cell<Pair<Int?, Int?>> =
        map(store.currentArea, store.currentAreaVariant) { area, variant ->
            Pair(area?.id, variant?.id)
        }

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
        map(store.selectedEvent, event.id) { selectedEvent, eventId ->
            selectedEvent?.id?.value == eventId
        }

    fun isMultiSelected(event: QuestEventModel): Cell<Boolean> =
        map(store.selectedEvents, event.id) { selectedEvents, eventId ->
            selectedEvents.any { it.id.value == eventId }
        }

    fun selectEvent(event: QuestEventModel?, ctrlKey: Boolean = false) {
        if (ctrlKey && event != null) {
            // Only toggle multi-selection, don't call setSelectedEvent
            store.toggleEventSelection(event)
        } else {
            // Clear multi-selection and set single selection
            store.setSelectedEvent(event)

            // Also select the section associated with this event
            if (event != null) {
                val currentQuest = store.currentQuest.value
                val currentAreaVariant = store.currentAreaVariant.value
                if (currentQuest != null && currentAreaVariant != null) {
                    // Use getLoadedSections to get sections that have been loaded
                    // This ensures we get the correct section even if it was loaded asynchronously
                    val sections = store.currentAreaSections.value
                    val eventSection = sections.find { it.id == event.sectionId.value }
                    store.setSelectedSection(eventSection)
                } else {
                    store.setSelectedSection(null)
                }
            } else {
                store.setSelectedSection(null)
            }
        }
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
                CreateEventCommand(
                    store,
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
                    DeleteEventCommand(store, quest, index, event)
                )
            }
        }
    }

    fun setId(event: QuestEventModel, id: Int) {
        store.executeAction(
            EditEventPropertyCommand(
                store,
                "Edit ID of event ${event.id.value}",
                event,
                QuestEventModel::setId,
                id,
                event.id.value,
            )
        )
    }

    fun setSectionId(event: QuestEventModel, sectionId: Int) {
        store.executeAction(
            EditEventPropertyCommand(
                store,
                "Edit section of event ${event.id.value}",
                event,
                QuestEventModel::setSectionId,
                sectionId,
                event.sectionId.value,
            )
        )
    }

    fun setWaveId(event: QuestEventModel, waveId: Int) {
        store.executeAction(
            EditEventPropertyCommand(
                store,
                "Edit wave of event ${event.id}",
                event,
                QuestEventModel::setWaveId,
                waveId,
                event.wave.value.id,
            )
        )
    }

    fun setDelay(event: QuestEventModel, delay: Int) {
        store.executeAction(
            EditEventPropertyCommand(
                store,
                "Edit delay of event ${event.id}",
                event,
                QuestEventModel::setDelay,
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

        store.executeAction(CreateEventActionCommand(store, event, action))
    }

    fun removeAction(event: QuestEventModel, action: QuestEventActionModel) {
        val index = event.actions.value.indexOf(action)
        store.executeAction(DeleteEventActionCommand(store, event, index, action))
    }

    fun canGoToEvent(eventId: Cell<Int>): Cell<Boolean> = store.canGoToEvent(eventId)

    fun goToEvent(eventId: Int) {
        store.goToEvent(eventId)
    }

    fun goToEventSection(event: QuestEventModel) {
        store.goToEventSection(event)
    }

    fun goToSection(sectionId: Int) {
        store.goToSection(sectionId)
        // TODO: Add camera navigation implementation here
        console.log("goToSection called for section $sectionId")
    }

    fun setActionSectionId(
        event: QuestEventModel,
        action: QuestEventActionModel.SpawnNpcs,
        sectionId: Int,
    ) {
        store.executeAction(
            EditEventPropertyCommand(
                store,
                "Edit action section",
                event,
                QuestEventModel::setSectionId,
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
            EditEventActionPropertyCommand(
                store,
                "Edit action appear flag",
                event,
                action,
                QuestEventActionModel.SpawnNpcs::setAppearFlag,
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
            EditEventActionPropertyCommand(
                store,
                "Edit action door",
                event,
                action,
                QuestEventActionModel.Door::setDoorId,
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
            EditEventActionPropertyCommand(
                store,
                "Edit action event",
                event,
                action,
                QuestEventActionModel.TriggerEvent::setEventId,
                eventId,
                action.eventId.value,
            )
        )
    }

    /**
     * Get a tooltip summarizing NPCs that belong to the given event.
     * NPCs are matched by area ID (or floor ID), wave ID, and section ID.
     * Returns a Cell<String?> that updates when NPCs or event properties change.
     */
    fun getEventNpcsSummary(event: QuestEventModel): Cell<String?> =
        map(store.currentQuest, event.wave, event.sectionId) { quest, wave, sectionId ->
            if (quest == null) return@map null

            // Event's areaId might be a floorId for quests with floor mappings
            val eventAreaId = event.areaId

            // Find NPCs that match this event's area, wave, and section
            val matchingNpcs = quest.npcs.value.filter { npc ->
                npc.areaId == eventAreaId &&
                        npc.wave.value.id == wave.id &&
                        npc.sectionId.value == sectionId
            }

            if (matchingNpcs.isEmpty()) return@map null

            // Group NPCs by type and count
            val npcCounts = matchingNpcs
                .groupBy { it.type }
                .map { (type, npcs) -> type.simpleName to npcs.size }
                .sortedByDescending { it.second }

            // Format as table with Name and Count columns
            val header = "Monster          Count"
            val separator = "-".repeat(22)
            val rows = npcCounts.joinToString("\n") { (name, count) ->
                name.padEnd(17) + count.toString()
            }
            "$header\n$separator\n$rows"
        }

    /**
     * Get a tooltip summarizing NPCs for all multi-selected events.
     * Returns a Cell<String?> that updates when selected events or their NPCs change.
     */
    fun getMultiSelectedEventNpcsSummary(): Cell<String?> =
        map(store.currentQuest, store.selectedEvents) { quest, selectedEvents ->
            if (quest == null || selectedEvents.size < 2) return@map null

            // Collect all matching NPCs from all selected events
            val allMatchingNpcs = selectedEvents.flatMap { event ->
                val eventAreaId = event.areaId
                quest.npcs.value.filter { npc ->
                    npc.areaId == eventAreaId &&
                            npc.wave.value.id == event.wave.value.id &&
                            npc.sectionId.value == event.sectionId.value
                }
            }

            if (allMatchingNpcs.isEmpty()) return@map null

            // Group NPCs by type and count
            val npcCounts = allMatchingNpcs
                .groupBy { it.type }
                .map { (type, npcs) -> type.simpleName to npcs.size }
                .sortedByDescending { it.second }

            // Format as table with Name and Count columns
            val header = "Monster          Count"
            val separator = "-".repeat(22)
            val rows = npcCounts.joinToString("\n") { (name, count) ->
                name.padEnd(17) + count.toString()
            }
            // Add count of selected events at the top
            "Selected: ${selectedEvents.size} events\n$header\n$separator\n$rows"
        }

    /**
     * Check if there are multiple events selected (for multi-selection mode).
     */
    fun hasMultiSelection(): Cell<Boolean> =
        store.selectedEvents.map { it.size >= 2 }
}
