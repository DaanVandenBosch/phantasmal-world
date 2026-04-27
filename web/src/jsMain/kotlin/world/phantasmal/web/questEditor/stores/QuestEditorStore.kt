package world.phantasmal.web.questEditor.stores

import kotlinx.coroutines.launch
import mu.KotlinLogging
import world.phantasmal.cell.*
import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.list.emptyListCell
import world.phantasmal.cell.list.filtered
import world.phantasmal.cell.list.flatMapToList
import world.phantasmal.core.externals.browser.FileSystemDirectoryHandle
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.asm.dataFlowAnalysis.FloorMapping
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.commands.Command
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.core.undo.UndoManager
import world.phantasmal.web.core.undo.UndoStack
import world.phantasmal.web.externals.three.Euler
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.web.questEditor.QuestRunner
import world.phantasmal.web.questEditor.loading.FreeRoamAreaInfo
import world.phantasmal.web.questEditor.loading.QuestLoader
import world.phantasmal.web.questEditor.models.*
import world.phantasmal.webui.stores.Store

private val logger = KotlinLogging.logger {}

class FreeRoamQuestResult(
    val questModel: QuestModel,
    val binName: String?,
    val datFilesByFloor: Map<Int, Triple<String, String, String>>,
)

class QuestEditorStore(
    private val questLoader: QuestLoader,
    uiStore: UiStore,
    private val areaStore: AreaStore,
    private val undoManager: UndoManager,
    private val viewportStore: ViewportStore,
    initializeNewQuest: Boolean,
) : Store() {
    private val _currentQuest = mutableCell<QuestModel?>(null)
    private val _currentArea = mutableCell<AreaModel?>(null)
    private val _currentAreaVariant = mutableCell<AreaVariantModel?>(null)
    private val _currentFloorIds = mutableCell<Set<Int>?>(null)
    private val _focusedEvent = mutableCell<QuestEventModel?>(null)
    private val _selectedEvents = mutableCell<Set<QuestEventModel>>(emptySet())
    private val _highlightedEntity = mutableCell<QuestEntityModel<*, *>?>(null)
    private val _selectedEntity = mutableCell<QuestEntityModel<*, *>?>(null)
    private val mainUndo = UndoStack(undoManager)
    private val _sectionsUpdated = mutableCell(0) // Trigger to update sections
    private val _selectedSection = mutableCell<SectionModel?>(null)

    private val runner = QuestRunner()
    val currentQuest: Cell<QuestModel?> = _currentQuest
    val currentArea: Cell<AreaModel?> = _currentArea
    val currentAreaVariant: Cell<AreaVariantModel?> = _currentAreaVariant
    /** When set, entity filtering uses these specific floor IDs instead of area+variant lookup. */
    val currentFloorIds: Cell<Set<Int>?> = _currentFloorIds

    val currentAreaNpcs: ListCell<QuestNpcModel> =
        flatMapToList(currentQuest, currentArea, currentFloorIds) { quest, area, floorIds ->
            filterEntitiesByFloor(quest, area, floorIds, quest?.npcs) { it.areaId }
        }

    val currentAreaObjects: ListCell<QuestObjectModel> =
        flatMapToList(currentQuest, currentArea, currentFloorIds) { quest, area, floorIds ->
            filterEntitiesByFloor(quest, area, floorIds, quest?.objects) { it.areaId }
        }

    val currentAreaEvents: ListCell<QuestEventModel> =
        flatMapToList(currentQuest, currentArea, currentFloorIds) { quest, area, floorIds ->
            filterEntitiesByFloor(quest, area, floorIds, quest?.events) { it.areaId }
        }

    val selectedEvent: Cell<QuestEventModel?> = _focusedEvent
    val selectedEvents: Cell<Set<QuestEventModel>> = _selectedEvents

    /**
     * Atomically updates both event selection cells, enforcing the invariant that
     * [focused] is null or contained in [events].
     */
    private fun updateEventSelection(events: Set<QuestEventModel>, focused: QuestEventModel?) {
        require(focused == null || events.any { it.id.value == focused.id.value }) {
            "Focused event must be null or contained in the selection set."
        }
        _selectedEvents.value = events
        _focusedEvent.value = focused
    }

    /**
     * Get section and wave info from selected events for NPC filtering.
     * Only NPCs that match both the section and wave of selected events will be shown.
     */
    val selectedEventsSectionWaves: Cell<Set<Pair<Int, Int>>> = selectedEvents.map { events ->
        events.map { event -> Pair(event.sectionId.value, event.wave.value.id) }.toSet()
    }

    /**
     * Get all sections for the current area variant for goto section functionality
     */
    val currentAreaSections: Cell<List<SectionModel>> =
        map(currentQuest, currentAreaVariant, _sectionsUpdated) { quest, areaVariant, _ ->
            if (quest != null && areaVariant != null) {
                (areaStore.getLoadedSections(quest.episode, areaVariant) ?: emptyList())
                    .sortedBy { it.id }
            } else {
                emptyList()
            }
        }

    /**
     * The entity the user is currently hovering over.
     */
    val highlightedEntity: Cell<QuestEntityModel<*, *>?> = _highlightedEntity

    /**
     * The entity the user has selected, typically by clicking it.
     */
    val selectedEntity: Cell<QuestEntityModel<*, *>?> = _selectedEntity

    val questEditingEnabled: Cell<Boolean> = currentQuest.isNotNull() and !runner.running

    val canUndo: Cell<Boolean> = questEditingEnabled and undoManager.canUndo
    val firstUndo: Cell<Command?> = undoManager.firstUndo
    val canRedo: Cell<Boolean> = questEditingEnabled and undoManager.canRedo
    val firstRedo: Cell<Command?> = undoManager.firstRedo

    /**
     * True if there have been changes since the last save.
     */
    val canSaveChanges: Cell<Boolean> = !undoManager.allAtSavePoint

    val selectedSection: Cell<SectionModel?> = _selectedSection

    init {
        observeNow(uiStore.currentTool) { tool ->
            if (tool == PwToolType.QuestEditor) {
                makeMainUndoCurrent()
            }
        }

        if (initializeNewQuest) {
            scope.launch { setCurrentQuest(getDefaultQuest(Episode.I)) }
        }

        // Clear entity selection when wave filters change and selected entities are no longer visible.
        observeNow(selectedEventsSectionWaves) { sectionWaves ->
            mutateDeferred {
                clearIncompatibleEntitySelections(sectionWaves)
            }
        }
    }

    /**
     * Resolves a floor ID to its area and variant using the quest's floor mappings.
     * Returns null if the quest has no floor mappings or no mapping exists for the given floor ID.
     */
    private fun resolveAreaVariantForFloor(
        quest: QuestModel,
        floorId: Int,
    ): Pair<AreaModel?, AreaVariantModel?>? {
        if (quest.floorMappings.isEmpty()) return null
        val mapping = quest.floorMappings.find { it.floorId == floorId } ?: return null
        val mapEp = mapping.mapEpisode ?: quest.episode
        val area = areaStore.getArea(mapEp, mapping.areaId)
        val variant = areaStore.getVariant(mapEp, mapping.areaId, mapping.variantId)
        return Pair(area, variant)
    }

    private fun <T> filterEntitiesByFloor(
        quest: QuestModel?,
        area: AreaModel?,
        floorIds: Set<Int>?,
        entities: ListCell<T>?,
        getAreaId: (T) -> Int,
    ): ListCell<T> {
        if (quest == null || area == null || entities == null) return emptyListCell()

        return if (floorIds != null) {
            entities.filtered { getAreaId(it) in floorIds }
        } else {
            // Fallback for quests without floor mappings (e.g. free roam).
            entities.filtered { getAreaId(it) == area.id }
        }
    }

    override fun dispose() {
        runner.stop()
        super.dispose()
    }

    fun makeMainUndoCurrent() {
        undoManager.setCurrent(mainUndo)
    }

    fun undo() {
        undoManager.undo()
    }

    fun redo() {
        undoManager.redo()
    }

    suspend fun setCurrentQuest(quest: QuestModel?) {
        undoManager.reset()

        runner.stop()

        if (quest == null) {
            mutate {
                _highlightedEntity.value = null
                _selectedEntity.value = null
                updateEventSelection(emptySet(), null)
                _selectedSection.value = null
                _currentArea.value = null
                _currentAreaVariant.value = null
                _currentFloorIds.value = null
                _currentQuest.value = null
            }
        } else {
            // Resolve before mutating so all cells update atomically.
            val (initialArea, initialVariant, initialFloorId) = resolveInitialAreaAndVariant(quest)

            mutate {
                _highlightedEntity.value = null
                _selectedEntity.value = null
                updateEventSelection(emptySet(), null)
                _selectedSection.value = null
                _currentQuest.value = quest
                _currentArea.value = initialArea
                _currentAreaVariant.value = initialVariant
                _currentFloorIds.value = initialFloorId?.let { setOf(it) }
            }

            // Load section data (suspend, must be outside mutate).
            updateQuestEntitySections(quest)

            // Ensure all entities have their section initialized.
            quest.npcs.value.forEach(QuestNpcModel::setSectionInitialized)
            quest.objects.value.forEach(QuestObjectModel::setSectionInitialized)

            // Trigger section loading for dropdown immediately after quest is loaded
            _sectionsUpdated.value += 1

            // Auto-select the section closest to the origin for the initial area.
            autoSelectClosestSection(quest, initialVariant)
        }
    }

    /**
     * Resolves the initial area, variant, and floor ID to display when a quest is first loaded.
     * For quests with floor mappings, prefers floor 0, then falls back to the first floor with
     * entities, then the first available floor.
     * For standard quests, uses area 0.
     *
     * @return Triple of (area, variant, floorId). floorId is null only for quests without floor mappings.
     */
    private fun resolveInitialAreaAndVariant(quest: QuestModel): Triple<AreaModel?, AreaVariantModel?, Int?> {
        if (quest.floorMappings.isNotEmpty()) {
            // For quests with floor mappings, find the mapping for floor 0 (starting area)
            val floor0Mapping = quest.floorMappings.find { it.floorId == 0 }
            if (floor0Mapping != null) {
                val resolved = resolveAreaVariantForFloor(quest, 0)
                if (resolved != null) {
                    return Triple(resolved.first, resolved.second, 0)
                }
            }

            // Fallback: find the first floor with entities
            for (mapping in quest.floorMappings) {
                val hasEntities =
                    quest.npcs.value.any { it.areaId == mapping.floorId } ||
                    quest.objects.value.any { it.areaId == mapping.floorId }
                if (hasEntities) {
                    val resolved = resolveAreaVariantForFloor(quest, mapping.floorId)
                    if (resolved != null) {
                        return Triple(resolved.first, resolved.second, mapping.floorId)
                    }
                }
            }

            // Last fallback: first available floor
            val first = quest.floorMappings.firstOrNull()
            if (first != null) {
                val resolved = resolveAreaVariantForFloor(quest, first.floorId)
                if (resolved != null) {
                    return Triple(resolved.first, resolved.second, first.floorId)
                }
            }

            return Triple(null, null, null)
        }

        val area = areaStore.getArea(quest.episode, 0)
        return Triple(area, area?.areaVariants?.getOrNull(0), null)
    }

    /**
     * Auto-selects the section closest to the origin for the given area variant.
     */
    private fun autoSelectClosestSection(quest: QuestModel, variant: AreaVariantModel?) {
        if (variant != null) {
            val sections = areaStore.getLoadedSections(quest.episode, variant)
            if (sections != null && sections.isNotEmpty()) {
                _selectedSection.value =
                    sections.minByOrNull { it.position.distanceTo(Vector3(0.0, 0.0, 0.0)) }
            }
        }
    }

    suspend fun getDefaultQuest(episode: Episode): QuestModel =
        convertQuestToModel(questLoader.loadDefaultQuest(episode), areaStore::getVariant)

    suspend fun getCityQuest(episode: Episode): QuestModel =
        convertQuestToModel(questLoader.loadCityQuest(episode), areaStore::getVariant)

    suspend fun getLobbyQuest(variant: Int): QuestModel =
        convertQuestToModel(questLoader.loadLobbyQuest(variant), areaStore::getVariant)

    suspend fun getFreeRoamQuest(
        gameDirHandle: FileSystemDirectoryHandle,
        info: FreeRoamAreaInfo,
        v1: Int = 0,
        v2: Int = 0,
    ): FreeRoamQuestResult {
        val result = questLoader.loadFreeRoamQuest(gameDirHandle, info, v1, v2)
        val questModel = convertQuestToModel(result.quest, areaStore::getVariant)
        return FreeRoamQuestResult(questModel, result.binName, result.datFilesByFloor)
    }

    fun <T> setQuestProperty(
        quest: QuestModel,
        setter: (QuestModel, T) -> Unit,
        value: T,
    ) {
        setter(quest, value)
    }

    fun setCurrentArea(area: AreaModel?) {
        val event = selectedEvent.value

        if (area != null && event != null && area.id != event.areaId) {
            setSelectedEvent(null)
        }

        _highlightedEntity.value = null
        _selectedEntity.value = null
        _currentArea.value = area
    }

    fun setCurrentAreaVariant(variant: AreaVariantModel?) {
        _currentAreaVariant.value = variant

        // Load sections for the new area variant if quest is loaded
        if (variant != null) {
            currentQuest.value?.let {
                requestSectionLoading(variant.episode, variant)
            }
        }
    }

    fun setCurrentFloorIds(floorIds: Set<Int>?) {
        _currentFloorIds.value = floorIds
    }

    /**
     * Switch to the floor containing [floorId]: sets area, variant, and floorIds.
     */
    private fun switchToFloor(quest: QuestModel, floorId: Int) {
        val resolved = resolveAreaVariantForFloor(quest, floorId)
        if (resolved != null) {
            _currentArea.value = resolved.first
            _currentAreaVariant.value = resolved.second
            _currentFloorIds.value = setOf(floorId)
        } else {
            val area = areaStore.getArea(quest.episode, floorId)
            _currentArea.value = area
            _currentAreaVariant.value = area?.areaVariants?.getOrNull(0)
            _currentFloorIds.value = if (area != null) setOf(floorId) else null
        }
    }

    fun addEvent(quest: QuestModel, index: Int, event: QuestEventModel) {
        mutate {
            quest.addEvent(index, event)
            setSelectedEvent(event)
        }
    }

    fun removeEvent(quest: QuestModel, event: QuestEventModel) {
        mutate {
            setSelectedEvent(null)
            quest.removeEvent(event)
        }
    }

    fun setSelectedEvent(event: QuestEventModel?) {
        event?.let {
            val wave = event.wave.value

            highlightedEntity.value?.let { entity ->
                if (entity is QuestNpcModel && entity.wave.value != wave) {
                    setHighlightedEntity(null)
                }
            }

            selectedEntity.value?.let { entity ->
                if (entity is QuestNpcModel && entity.wave.value != wave) {
                    setSelectedEntity(null)
                }
            }

            // Cross-area navigation: switch to the event's floor if different.
            val quest = currentQuest.value

            if (quest != null && _currentFloorIds.value?.let { event.areaId !in it } != false) {
                mutate {
                    switchToFloor(quest, event.areaId)
                }
            }
        }

        if (event != null) {
            updateEventSelection(setOf(event), event)
        } else {
            updateEventSelection(emptySet(), null)
        }
    }

    /**
     * Toggle event selection for multi-selection with Ctrl+click
     */
    fun toggleEventSelection(event: QuestEventModel) {
        val eventId = event.id.value
        val oldSelection = _selectedEvents.value
        val wasInSelection = oldSelection.any { it.id.value == eventId }

        val newSet = if (wasInSelection) {
            oldSelection.filterNot { it.id.value == eventId }.toSet()
        } else {
            oldSelection + event
        }

        val newFocused = when {
            newSet.isEmpty() -> null
            !wasInSelection -> event
            _focusedEvent.value?.id?.value == eventId -> newSet.firstOrNull()
            else -> _focusedEvent.value
        }

        updateEventSelection(newSet, newFocused)
    }

    fun <T> setEventProperty(
        event: QuestEventModel,
        setter: (QuestEventModel, T) -> Unit,
        value: T,
    ) {
        mutate {
            // Preserve multi-selection state when editing properties
            ensureEventInSelection(event)
            setter(event, value)
        }
    }

    fun addEventAction(event: QuestEventModel, action: QuestEventActionModel) {
        mutate {
            // Preserve multi-selection state when adding actions
            ensureEventInSelection(event)
            event.addAction(action)
        }
    }

    fun addEventAction(event: QuestEventModel, index: Int, action: QuestEventActionModel) {
        mutate {
            // Preserve multi-selection state when adding actions
            ensureEventInSelection(event)
            event.addAction(index, action)
        }
    }

    fun removeEventAction(event: QuestEventModel, action: QuestEventActionModel) {
        mutate {
            // Preserve multi-selection state when removing actions
            ensureEventInSelection(event)
            event.removeAction(action)
        }
    }

    /**
     * Ensures the event is in the selection without clearing multi-selection.
     * If multi-selection is active and the event is already selected, just update focused.
     * Otherwise, delegate to setSelectedEvent (replaces the entire selection).
     */
    private fun ensureEventInSelection(event: QuestEventModel) {
        val currentSelection = _selectedEvents.value
        val eventId = event.id.value

        if (currentSelection.any { it.id.value == eventId }) {
            _focusedEvent.value = event
        } else {
            setSelectedEvent(event)
        }
    }

    fun <Action : QuestEventActionModel, T> setEventActionProperty(
        event: QuestEventModel,
        action: Action,
        setter: (Action, T) -> Unit,
        value: T,
    ) {
        mutate {
            // Preserve multi-selection state when editing action properties
            ensureEventInSelection(event)
            setter(action, value)
        }
    }

    fun setHighlightedEntity(entity: QuestEntityModel<*, *>?) {
        _highlightedEntity.value = entity
    }

    fun setSelectedEntity(entity: QuestEntityModel<*, *>?) {
        mutate {
            entity?.let {
                currentQuest.value?.let { quest ->
                    switchToFloor(quest, entity.areaId)
                }
            }
            _selectedEntity.value = entity
        }
    }

    fun addEntity(quest: QuestModel, entity: QuestEntityModel<*, *>) {
        mutate {
            quest.addEntity(entity)
            setSelectedEntity(entity)
        }
    }

    fun removeEntity(quest: QuestModel, entity: QuestEntityModel<*, *>) {
        mutate {
            if (entity == _selectedEntity.value) {
                _selectedEntity.value = null
            }

            quest.removeEntity(entity)
        }
    }

    fun setEntityPosition(entity: QuestEntityModel<*, *>, sectionId: Int?, position: Vector3) {
        mutate {
            setSelectedEntity(entity)
            sectionId?.let { setEntitySection(entity, it) }
            entity.setPosition(position)
        }
    }

    fun setEntityWorldPosition(entity: QuestEntityModel<*, *>, sectionId: Int?, position: Vector3) {
        mutate {
            setSelectedEntity(entity)
            sectionId?.let { setEntitySection(entity, it) }
            entity.setWorldPosition(position)
        }
    }

    fun setEntityRotation(entity: QuestEntityModel<*, *>, rotation: Euler) {
        mutate {
            setSelectedEntity(entity)
            entity.setRotation(rotation)
        }
    }

    fun setEntityWorldRotation(entity: QuestEntityModel<*, *>, rotation: Euler) {
        mutate {
            setSelectedEntity(entity)
            entity.setWorldRotation(rotation)
        }
    }

    fun <Entity : QuestEntityModel<*, *>, T> setEntityProperty(
        entity: Entity,
        setter: (Entity, T) -> Unit,
        value: T,
    ) {
        mutate {
            setSelectedEntity(entity)
            setter(entity, value)
        }
    }

    fun setEntityProp(entity: QuestEntityModel<*, *>, prop: QuestEntityPropModel, value: Any) {
        mutate {
            setSelectedEntity(entity)
            prop.setValue(value)
        }
    }

    suspend fun setFloorMappings(floorMappings: List<FloorMapping>) {
        currentQuest.value?.let { quest ->
            quest.setFloorMappings(floorMappings)
            updateQuestEntitySections(quest)
        }
    }

    fun setEntitySectionId(entity: QuestEntityModel<*, *>, sectionId: Int) {
        mutate {
            setSelectedEntity(entity)
            entity.setSectionId(sectionId)
        }
    }

    fun setEntitySection(entity: QuestEntityModel<*, *>, section: SectionModel) {
        mutate {
            setSelectedEntity(entity)
            entity.setSection(section)
        }
    }

    /**
     * Sets [QuestEntityModel.sectionId] and [QuestEntityModel.section] if there's a section with
     * [sectionId] as ID.
     */
    private fun setEntitySection(entity: QuestEntityModel<*, *>, sectionId: Int) {
        currentQuest.value?.let { quest ->
            // For multi-floor quests, entity.areaId is a floor ID — use floorToVariantMap
            val variant = quest.floorToVariantMap[entity.areaId]
            val variants = if (variant != null) {
                listOf(variant)
            } else {
                quest.areaVariants.value.filter { it.area.id == entity.areaId }
            }

            for (areaVariant in variants) {
                val section = areaStore.getLoadedSections(areaVariant.episode, areaVariant)
                    ?.find { it.id == sectionId }

                if (section != null) {
                    entity.setSection(section)
                    return@let
                }
            }

            // If section not found in any variant, just set the ID
            entity.setSectionId(sectionId)
        }
    }

    fun executeAction(command: Command) {
        pushAction(command)
        command.execute()
    }

    fun pushAction(command: Command) {
        require(questEditingEnabled.value) {
            val reason = when {
                currentQuest.value == null -> " (no current quest)"
                runner.running.value -> " (QuestRunner is running)"
                else -> ""
            }
            "Quest editing is disabled at the moment$reason."
        }
        mainUndo.push(command)
    }

    /**
     * Selects the section associated with the given event, or clears the section selection
     * if the event is null or no matching section is found.
     */
    fun selectSectionForEvent(event: QuestEventModel?) {
        if (event != null) {
            val quest = currentQuest.value
            val variant = currentAreaVariant.value
            if (quest != null && variant != null) {
                // Use getLoadedSections to get sections that have been loaded
                // This ensures we get the correct section even if it was loaded asynchronously
                val sections = currentAreaSections.value
                val eventSection = sections.find { it.id == event.sectionId.value }
                _selectedSection.value = eventSection
            } else {
                _selectedSection.value = null
            }
        } else {
            _selectedSection.value = null
        }
    }

    fun setSelectedSection(section: SectionModel?) {
        _selectedSection.value = section
    }

    fun questSaved() {
        undoManager.savePoint()
    }

    /**
     * Request async loading of sections for a specific area variant
     */
    fun requestSectionLoading(episode: Episode, areaVariant: AreaVariantModel) {
        scope.launch {
            try {
                val sections = areaStore.getSections(episode, areaVariant)
                // Batch both cell updates atomically so observers see a consistent state.
                mutate {
                    _sectionsUpdated.value += 1
                    if (_selectedSection.value == null && sections.isNotEmpty()) {
                        val closest = sections.minByOrNull {
                            it.position.distanceTo(Vector3(0.0, 0.0, 0.0))
                        }
                        _selectedSection.value = closest
                    }
                }
            } catch (e: Exception) {
                logger.warn(e) { "Error loading sections for area variant ${areaVariant.id}" }
            }
        }
    }

    /**
     * True if the event exists in the current area and quest editing is enabled.
     */
    fun canGoToEvent(eventId: Cell<Int>): Cell<Boolean> =
        map(questEditingEnabled, currentAreaEvents, eventId) { en, evts, id ->
            en && evts.any { it.id.value == id }
        }

    fun goToEvent(eventId: Int) {
        currentAreaEvents.value.find { it.id.value == eventId }?.let { event ->
            setSelectedEvent(event)
        }
    }

    /**
     * Navigate camera to a specific section by section ID.
     */
    fun goToSection(sectionId: Int) {
        currentAreaVariant.value?.let { areaVariant ->
            // Use quest episode if available, otherwise default to Episode I
            val episode = currentQuest.value?.episode ?: Episode.I
            val sections = areaStore.getLoadedSections(episode, areaVariant)
            sections?.find { it.id == sectionId }?.let { section ->
                // Set target camera position without using observers to avoid circular dependencies
                viewportStore.setTargetCameraPosition(section.position.clone())
            }
        }
    }

    /**
     * Navigate camera to the section of a specific event.
     */
    fun goToEventSection(event: QuestEventModel) {
        goToSection(event.sectionId.value)
    }

    /**
     * Clear entity selections that are incompatible with the current wave filters.
     * Called asynchronously to avoid circular dependencies.
     */
    private fun clearIncompatibleEntitySelections(sectionWaves: Set<Pair<Int, Int>>) {
        if (sectionWaves.isEmpty()) return

        if (!isEntityCompatibleWithWaveFilters(_selectedEntity.value, sectionWaves)) {
            _selectedEntity.value = null
        }
        if (!isEntityCompatibleWithWaveFilters(_highlightedEntity.value, sectionWaves)) {
            _highlightedEntity.value = null
        }
    }

    private fun isEntityCompatibleWithWaveFilters(
        entity: QuestEntityModel<*, *>?,
        sectionWaves: Set<Pair<Int, Int>>,
    ): Boolean {
        if (entity == null) return true
        val waveId = (entity as? QuestNpcModel)?.wave?.value?.id ?: return true
        return Pair(entity.sectionId.value, waveId) in sectionWaves
    }

    private suspend fun updateQuestEntitySections(quest: QuestModel) {
        quest.areaVariants.value.forEach { variant ->
            val sections = areaStore.getSections(variant.episode, variant)
            variant.setSections(sections)
            setSectionOnQuestEntities(quest, quest.npcs.value, variant, sections)
            setSectionOnQuestEntities(quest, quest.objects.value, variant, sections)
        }
    }

    private fun setSectionOnQuestEntities(
        quest: QuestModel,
        entities: List<QuestEntityModel<*, *>>,
        variant: AreaVariantModel,
        sections: List<SectionModel>,
    ) {

        entities.forEach { entity ->
            if (quest.entityBelongsToArea(entity.areaId, variant.area.id, variant.id)) {
                val section = sections.find { it.id == entity.sectionId.value }

                if (section == null) {
                    logger.warn { "Section ${entity.sectionId.value} not found." }
                    entity.setSectionInitialized()
                } else {
                    entity.setSection(section, keepRelativeTransform = true)
                }
            }
        }
    }

}
