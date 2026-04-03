package world.phantasmal.web.questEditor.controllers

import kotlinx.browser.window
import world.phantasmal.cell.*
import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.list.listCell
import world.phantasmal.core.disposable.disposable
import world.phantasmal.web.questEditor.commands.*
import world.phantasmal.web.questEditor.models.QuestEventActionModel
import world.phantasmal.web.questEditor.models.QuestEventModel
import world.phantasmal.web.questEditor.models.QuestNpcModel
import world.phantasmal.web.questEditor.stores.PlaybackVisualizationStore
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.controllers.Controller

enum class PlaybackState {
    Stopped,
    Playing,
    Paused,
}

class EventsController(
    private val store: QuestEditorStore,
    private val playbackVisualizationStore: PlaybackVisualizationStore,
) : Controller() {
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
        // Stop playback when area changes.
        observe(store.currentArea) {
            mutateDeferred { stopPlayback() }
        }

        // Push playback info to store for 3D view overlay and door/spawn highlighting.
        observe(map(_playbackIndex, events) { index, evts ->
            if (index >= 0 && index in evts.indices) evts[index] else null
        }) { event ->
            mutateDeferred {
                if (event != null) {
                    playbackVisualizationStore.setPlaybackActionText(describeEventActions(event))
                    playbackVisualizationStore.setPlaybackDoorIds(extractUnlockDoorIds(event))
                    playbackVisualizationStore.setPlaybackLockDoorIds(extractLockDoorIds(event))
                    playbackVisualizationStore.setPlaybackSpawnSectionIds(extractSpawnSectionIds(event))
                } else {
                    playbackVisualizationStore.setPlaybackActionText("")
                    playbackVisualizationStore.setPlaybackDoorIds(emptySet())
                    playbackVisualizationStore.setPlaybackLockDoorIds(emptySet())
                    playbackVisualizationStore.setPlaybackSpawnSectionIds(emptySet())
                }
            }
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

    private inline fun <reified A : QuestEventActionModel> extractActionValues(
        event: QuestEventModel,
        getValue: (A) -> Int,
    ): Set<Int> =
        event.actions.value
            .filterIsInstance<A>()
            .map(getValue)
            .toSet()

    private fun extractUnlockDoorIds(event: QuestEventModel): Set<Int> =
        extractActionValues<QuestEventActionModel.Door.Unlock>(event) { it.doorId.value }

    private fun extractLockDoorIds(event: QuestEventModel): Set<Int> =
        extractActionValues<QuestEventActionModel.Door.Lock>(event) { it.doorId.value }

    private fun extractSpawnSectionIds(event: QuestEventModel): Set<Int> =
        extractActionValues<QuestEventActionModel.SpawnNpcs>(event) { it.sectionId.value }

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
            store.selectSectionForEvent(event)

            // Show spawn/lock/unlock flash effects for the clicked event.
            // During active playback the observer on _playbackIndex handles this.
            // Also show effects when Paused — the user may click events to inspect them.
            if (_playbackState.value != PlaybackState.Playing) {
                showEventActionEffects(event)
            }
        }
    }

    /**
     * Pushes the given event's spawn/lock/unlock action data to the store so that
     * [EntityMeshManager] renders the corresponding labels with flash animations.
     */
    private fun showEventActionEffects(event: QuestEventModel?) {
        if (event != null) {
            // Use a sentinel clear + set in one deferred batch to ensure re-clicking the same
            // event still triggers observers (SimpleCell deduplicates identical values).
            mutateDeferred {
                // Clear
                playbackVisualizationStore.setPlaybackActionText("")
                playbackVisualizationStore.setPlaybackDoorIds(emptySet())
                playbackVisualizationStore.setPlaybackLockDoorIds(emptySet())
                playbackVisualizationStore.setPlaybackSpawnSectionIds(emptySet())
            }
            mutateDeferred {
                // Set new values
                playbackVisualizationStore.setPlaybackActionText(describeEventActions(event))
                playbackVisualizationStore.setPlaybackDoorIds(extractUnlockDoorIds(event))
                playbackVisualizationStore.setPlaybackLockDoorIds(extractLockDoorIds(event))
                playbackVisualizationStore.setPlaybackSpawnSectionIds(extractSpawnSectionIds(event))
            }
        } else {
            mutateDeferred {
                playbackVisualizationStore.setPlaybackActionText("")
                playbackVisualizationStore.setPlaybackDoorIds(emptySet())
                playbackVisualizationStore.setPlaybackLockDoorIds(emptySet())
                playbackVisualizationStore.setPlaybackSpawnSectionIds(emptySet())
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
                        sectionId = DEFAULT_SECTION_ID,
                        waveId = DEFAULT_WAVE_ID,
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
        editEventProperty(event, "Edit ID", QuestEventModel::setId, id, event.id.value)
    }

    fun setSectionId(event: QuestEventModel, sectionId: Int) {
        editEventProperty(event, "Edit section", QuestEventModel::setSectionId, sectionId, event.sectionId.value)
    }

    fun setWaveId(event: QuestEventModel, waveId: Int) {
        editEventProperty(event, "Edit wave", QuestEventModel::setWaveId, waveId, event.wave.value.id)
    }

    fun setDelay(event: QuestEventModel, delay: Int) {
        editEventProperty(event, "Edit delay", QuestEventModel::setDelay, delay, event.delay.value)
    }

    fun setCmMinEnemies(event: QuestEventModel, value: Int) {
        editEventProperty(event, "Edit CM min enemies", QuestEventModel::setCmMinEnemies, value, event.cmMinEnemies.value)
    }

    fun setCmMaxEnemies(event: QuestEventModel, value: Int) {
        editEventProperty(event, "Edit CM max enemies", QuestEventModel::setCmMaxEnemies, value, event.cmMaxEnemies.value)
    }

    fun setCmMaxWaves(event: QuestEventModel, value: Int) {
        editEventProperty(event, "Edit CM max waves", QuestEventModel::setCmMaxWaves, value, event.cmMaxWaves.value)
    }

    private fun <T> editEventProperty(
        event: QuestEventModel,
        description: String,
        setter: (QuestEventModel, T) -> Unit,
        newValue: T,
        oldValue: T,
    ) {
        store.executeAction(
            EditEventPropertyCommand(
                store,
                "$description of event ${event.id.value}",
                event,
                setter,
                newValue,
                oldValue,
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
    }

    fun setActionSectionId(event: QuestEventModel, action: QuestEventActionModel.SpawnNpcs, sectionId: Int) {
        editActionProperty(event, action, "Edit action section", QuestEventActionModel.SpawnNpcs::setSectionId, sectionId, action.sectionId.value)
    }

    fun setActionAppearFlag(event: QuestEventModel, action: QuestEventActionModel.SpawnNpcs, appearFlag: Int) {
        editActionProperty(event, action, "Edit action appear flag", QuestEventActionModel.SpawnNpcs::setAppearFlag, appearFlag, action.appearFlag.value)
    }

    fun setActionDoorId(event: QuestEventModel, action: QuestEventActionModel.Door, doorId: Int) {
        editActionProperty(event, action, "Edit action door", QuestEventActionModel.Door::setDoorId, doorId, action.doorId.value)
    }

    fun setActionEventId(event: QuestEventModel, action: QuestEventActionModel.TriggerEvent, eventId: Int) {
        editActionProperty(event, action, "Edit action event", QuestEventActionModel.TriggerEvent::setEventId, eventId, action.eventId.value)
    }

    private fun <A : QuestEventActionModel, T> editActionProperty(
        event: QuestEventModel,
        action: A,
        description: String,
        setter: (A, T) -> Unit,
        newValue: T,
        oldValue: T,
    ) {
        store.executeAction(
            EditEventActionPropertyCommand(store, description, event, action, setter, newValue, oldValue)
        )
    }

    /**
     * Get a tooltip summarizing NPCs that belong to the given event.
     * NPCs are matched by area ID (or floor ID), wave ID, and section ID.
     * Returns a Cell<String?> that updates when NPCs or event properties change.
     */
    fun getEventNpcsSummary(event: QuestEventModel): Cell<String?> =
        map(store.currentAreaNpcs, event.wave, event.sectionId) { npcs, wave, sectionId ->
            val matchingNpcs = npcs.filter { npc ->
                npc.areaId == event.areaId &&
                        npc.wave.value.id == wave.id &&
                        npc.sectionId.value == sectionId
            }

            formatNpcSummary(matchingNpcs)
        }

    fun getMultiSelectedEventNpcsSummary(): Cell<String?> =
        map(store.currentAreaNpcs, store.selectedEvents) { npcs, selectedEvents ->
            if (selectedEvents.size < 2) return@map null

            val allMatchingNpcs = selectedEvents.flatMap { event ->
                npcs.filter { npc ->
                    npc.areaId == event.areaId &&
                            npc.wave.value.id == event.wave.value.id &&
                            npc.sectionId.value == event.sectionId.value
                }
            }

            formatNpcSummary(allMatchingNpcs, prefix = "Selected: ${selectedEvents.size} events")
        }

    private fun formatNpcSummary(
        npcs: List<QuestNpcModel>,
        prefix: String? = null,
    ): String? {
        if (npcs.isEmpty()) return null

        val npcCounts = npcs
            .groupBy { it.type }
            .entries
            .map { it.key.simpleName to it.value.size }
            .sortedByDescending { it.second }

        val header = "Monster          Count"
        val separator = "-".repeat(22)
        val rows = npcCounts.joinToString("\n") { (name, count) ->
            name.padEnd(17) + count.toString()
        }
        return if (prefix != null) "$prefix\n$header\n$separator\n$rows"
        else "$header\n$separator\n$rows"
    }

    /**
     * Check if there are multiple events selected (for multi-selection mode).
     */
    fun hasMultiSelection(): Cell<Boolean> =
        store.selectedEvents.map { it.size >= 2 }

    companion object {
        private const val PLAYBACK_STEP_DELAY_MS = 2000
        private const val DEFAULT_SECTION_ID = 1
        private const val DEFAULT_WAVE_ID = 1
    }
}
