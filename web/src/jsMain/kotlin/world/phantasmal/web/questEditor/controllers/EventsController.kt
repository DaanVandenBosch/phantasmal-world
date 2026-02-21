package world.phantasmal.web.questEditor.controllers

import kotlinx.browser.window
import world.phantasmal.cell.*
import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.list.listCell
import world.phantasmal.core.disposable.disposable
import world.phantasmal.web.questEditor.commands.*
import world.phantasmal.web.questEditor.models.QuestEventActionModel
import world.phantasmal.web.questEditor.models.QuestEventModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.controllers.Controller

enum class PlaybackState {
    Stopped,
    Playing,
    Paused,
}

class EventsController(private val store: QuestEditorStore) : Controller() {
    val unavailable: Cell<Boolean> = store.currentQuest.isNull()
    val enabled: Cell<Boolean> = store.questEditingEnabled
    val removeEventEnabled: Cell<Boolean> = enabled and store.selectedEvent.isNotNull()
    val events: ListCell<QuestEventModel> = store.currentAreaEvents

    // Playback state
    private val _playbackState: MutableCell<PlaybackState> = mutableCell(PlaybackState.Stopped)
    private val _playbackIndex: MutableCell<Int> = mutableCell(-1)
    private var playbackTimerId: Int? = null

    val playbackState: Cell<PlaybackState> = _playbackState
    val playbackIndex: Cell<Int> = _playbackIndex

    val isStopped: Cell<Boolean> = _playbackState.map { it == PlaybackState.Stopped }
    val isPlaying: Cell<Boolean> = _playbackState.map { it == PlaybackState.Playing }

    val playEnabled: Cell<Boolean> =
        map(_playbackState, events) { state, evts ->
            state != PlaybackState.Playing && evts.isNotEmpty()
        }

    val pauseEnabled: Cell<Boolean> = isPlaying

    val stopEnabled: Cell<Boolean> = _playbackState.map { it != PlaybackState.Stopped }

    val stepForwardEnabled: Cell<Boolean> =
        map(_playbackState, _playbackIndex, events) { state, index, evts ->
            state != PlaybackState.Playing && evts.isNotEmpty() && index < evts.size - 1
        }

    val stepBackwardEnabled: Cell<Boolean> =
        map(_playbackState, _playbackIndex) { state, index ->
            state == PlaybackState.Paused && index > 0
        }

    val playbackStatusText: Cell<String> =
        map(_playbackIndex, events) { index, evts ->
            if (index >= 0 && index in evts.indices) {
                "Event ${index + 1} / ${evts.size}"
            } else {
                ""
            }
        }

    init {
        // Stop playback when area/events list changes.
        // Deferred via setTimeout to avoid "A cell is already changing" when
        // setCurrentArea sets _currentArea and this observer fires synchronously.
        observe(store.currentArea) { window.setTimeout({ stopPlayback() }, 0) }

        // Push playback info to store for 3D view overlay and door/spawn highlighting.
        // Deferred via setTimeout to avoid "A cell is already changing" when _playbackIndex
        // is set inside play()/stepForward()/stepBackward() and this observer fires
        // synchronously during the notification cycle.
        observe(map(_playbackIndex, events) { index, evts ->
            if (index >= 0 && index in evts.indices) evts[index] else null
        }) { event ->
            window.setTimeout({
                if (event != null) {
                    store.setPlaybackActionText(describeEventActions(event))
                    store.setPlaybackDoorIds(extractUnlockDoorIds(event))
                    store.setPlaybackLockDoorIds(extractLockDoorIds(event))
                    store.setPlaybackSpawnSectionIds(extractSpawnSectionIds(event))
                } else {
                    store.setPlaybackActionText("")
                    store.setPlaybackDoorIds(emptySet())
                    store.setPlaybackLockDoorIds(emptySet())
                    store.setPlaybackSpawnSectionIds(emptySet())
                }
            }, 0)
        }

        // Cancel timer on dispose.
        addDisposable(disposable { cancelTimer() })
    }

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

    fun play() {
        val evts = events.value
        if (evts.isEmpty()) return

        val startIndex = when (_playbackState.value) {
            PlaybackState.Paused -> _playbackIndex.value
            else -> 0
        }

        _playbackState.value = PlaybackState.Playing
        _playbackIndex.value = startIndex
        applyPlaybackSelection()
        scheduleNextStep()
    }

    fun pause() {
        if (_playbackState.value == PlaybackState.Playing) {
            cancelTimer()
            _playbackState.value = PlaybackState.Paused
        }
    }

    fun stopPlayback() {
        if (_playbackState.value == PlaybackState.Stopped) return

        cancelTimer()
        _playbackIndex.value = -1
        _playbackState.value = PlaybackState.Stopped
        selectEvent(null)
    }

    fun stepForward() {
        val evts = events.value
        if (evts.isEmpty()) return

        if (_playbackState.value == PlaybackState.Playing) {
            cancelTimer()
            _playbackState.value = PlaybackState.Paused
        }

        val nextIndex = _playbackIndex.value + 1
        if (nextIndex < evts.size) {
            if (_playbackState.value == PlaybackState.Stopped) {
                _playbackState.value = PlaybackState.Paused
            }
            _playbackIndex.value = nextIndex
            applyPlaybackSelection()
        }
    }

    fun stepBackward() {
        if (_playbackState.value != PlaybackState.Paused) return

        val prevIndex = _playbackIndex.value - 1
        if (prevIndex >= 0) {
            _playbackIndex.value = prevIndex
            applyPlaybackSelection()
        }
    }

    /** Called when user clicks an event during playback - pauses auto-play. */
    fun onEventClickedDuringPlayback(clickedEvent: QuestEventModel) {
        if (_playbackState.value == PlaybackState.Playing) {
            cancelTimer()
            _playbackState.value = PlaybackState.Paused
            val idx = events.value.indexOf(clickedEvent)
            if (idx >= 0) {
                _playbackIndex.value = idx
            }
        }
    }

    private fun applyPlaybackSelection() {
        val evts = events.value
        val index = _playbackIndex.value
        if (index in evts.indices) {
            val event = evts[index]
            selectEvent(event)
            // Navigate camera to current event's section
            store.goToEventSection(event)
        }
    }

    private fun scheduleNextStep() {
        cancelTimer()

        val evts = events.value
        val index = _playbackIndex.value
        if (index >= evts.size - 1) {
            // Reached the end of events - stop playback entirely
            stopPlayback()
            return
        }

        playbackTimerId = window.setTimeout({
            playbackTimerId = null
            if (_playbackState.value == PlaybackState.Playing) {
                val nextIndex = _playbackIndex.value + 1
                val currentEvts = events.value
                if (nextIndex < currentEvts.size) {
                    _playbackIndex.value = nextIndex
                    applyPlaybackSelection()
                    scheduleNextStep()
                } else {
                    stopPlayback()
                }
            }
        }, PLAYBACK_STEP_DELAY_MS)
    }

    private fun describeEventActions(event: QuestEventModel): String {
        val parts = event.actions.value.map { action ->
            when (action) {
                is QuestEventActionModel.SpawnNpcs ->
                    "Spawn (Sec ${action.sectionId.value}, Flag ${action.appearFlag.value})"
                is QuestEventActionModel.Door.Unlock ->
                    "Unlock Door #${action.doorId.value}"
                is QuestEventActionModel.Door.Lock ->
                    "Lock Door #${action.doorId.value}"
                is QuestEventActionModel.TriggerEvent ->
                    "Trigger Event #${action.eventId.value}"
            }
        }
        return if (parts.isEmpty()) "(no actions)" else parts.joinToString(" | ")
    }

    private fun extractUnlockDoorIds(event: QuestEventModel): Set<Int> =
        event.actions.value
            .filterIsInstance<QuestEventActionModel.Door.Unlock>()
            .map { it.doorId.value }
            .toSet()

    private fun extractLockDoorIds(event: QuestEventModel): Set<Int> =
        event.actions.value
            .filterIsInstance<QuestEventActionModel.Door.Lock>()
            .map { it.doorId.value }
            .toSet()

    private fun extractSpawnSectionIds(event: QuestEventModel): Set<Int> =
        event.actions.value
            .filterIsInstance<QuestEventActionModel.SpawnNpcs>()
            .map { it.sectionId.value }
            .toSet()

    private fun cancelTimer() {
        playbackTimerId?.let { window.clearTimeout(it) }
        playbackTimerId = null
    }

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

            // Show spawn/lock/unlock flash effects for the clicked event.
            // During active playback the observer on _playbackIndex handles this.
            if (_playbackState.value == PlaybackState.Stopped) {
                showEventActionEffects(event)
            }
        }
    }

    /**
     * Pushes the given event's spawn/lock/unlock action data to the store so that
     * [EntityMeshManager] renders the corresponding labels with flash animations.
     */
    private fun showEventActionEffects(event: QuestEventModel?) {
        // Clear first so that re-clicking the same event still triggers observers
        // (SimpleCell deduplicates identical values).
        store.setPlaybackActionText("")
        store.setPlaybackDoorIds(emptySet())
        store.setPlaybackLockDoorIds(emptySet())
        store.setPlaybackSpawnSectionIds(emptySet())

        window.setTimeout({
            if (event != null) {
                store.setPlaybackActionText(describeEventActions(event))
                store.setPlaybackDoorIds(extractUnlockDoorIds(event))
                store.setPlaybackLockDoorIds(extractLockDoorIds(event))
                store.setPlaybackSpawnSectionIds(extractSpawnSectionIds(event))
            }
        }, 0)
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

    fun setCmMinEnemies(event: QuestEventModel, value: Int) {
        store.executeAction(
            EditEventPropertyCommand(
                store,
                "Edit CM min enemies of event ${event.id}",
                event,
                QuestEventModel::setCmMinEnemies,
                value,
                event.cmMinEnemies.value,
            )
        )
    }

    fun setCmMaxEnemies(event: QuestEventModel, value: Int) {
        store.executeAction(
            EditEventPropertyCommand(
                store,
                "Edit CM max enemies of event ${event.id}",
                event,
                QuestEventModel::setCmMaxEnemies,
                value,
                event.cmMaxEnemies.value,
            )
        )
    }

    fun setCmMaxWaves(event: QuestEventModel, value: Int) {
        store.executeAction(
            EditEventPropertyCommand(
                store,
                "Edit CM max waves of event ${event.id}",
                event,
                QuestEventModel::setCmMaxWaves,
                value,
                event.cmMaxWaves.value,
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

    companion object {
        private const val PLAYBACK_STEP_DELAY_MS = 2000
    }
}
