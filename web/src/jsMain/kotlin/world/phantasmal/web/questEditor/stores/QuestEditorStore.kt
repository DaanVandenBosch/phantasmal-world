package world.phantasmal.web.questEditor.stores

import kotlinx.browser.window
import kotlinx.coroutines.launch
import mu.KotlinLogging
import world.phantasmal.cell.*
import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.list.emptyListCell
import world.phantasmal.cell.list.filtered
import world.phantasmal.cell.list.flatMapToList
import world.phantasmal.psolib.Episode
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.commands.Command
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.core.undo.UndoManager
import world.phantasmal.web.core.undo.UndoStack
import world.phantasmal.web.externals.three.Euler
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.web.questEditor.QuestRunner
import world.phantasmal.web.questEditor.loading.QuestLoader
import world.phantasmal.web.questEditor.models.*
import world.phantasmal.webui.stores.Store

private val logger = KotlinLogging.logger {}

class QuestEditorStore(
    private val questLoader: QuestLoader,
    uiStore: UiStore,
    private val areaStore: AreaStore,
    private val undoManager: UndoManager,
    initializeNewQuest: Boolean,
) : Store() {
    private val _devMode = mutableCell(false)
    private val _showSectionIds = mutableCell(true) // Section ID display toggle
    private val _spawnMonstersOnGround = mutableCell(false) // Monster ground spawn toggle
    private val _omnispawn = mutableCell(false) // Omnispawn toggle for NPCs
    private val _showOriginPoint = mutableCell(false) // Origin point display toggle
    private val _currentQuest = mutableCell<QuestModel?>(null)
    private val _currentArea = mutableCell<AreaModel?>(null)
    private val _currentAreaVariant = mutableCell<AreaVariantModel?>(null)
    private val _selectedEvent = mutableCell<QuestEventModel?>(null)
    private val _selectedEvents = mutableCell<Set<QuestEventModel>>(emptySet())
    private val _highlightedEntity = mutableCell<QuestEntityModel<*, *>?>(null)
    private val _selectedEntity = mutableCell<QuestEntityModel<*, *>?>(null)
    private val mainUndo = UndoStack(undoManager)
    private val _showCollisionGeometry = mutableCell(true)
    private val _mouseWorldPosition = mutableCell<Vector3?>(null)
    private val _targetCameraPosition = mutableCell<Vector3?>(null)
    private val _sectionsUpdated = mutableCell(0) // Trigger to update sections
    private val _selectedSection = mutableCell<SectionModel?>(null)
    

    val devMode: Cell<Boolean> = _devMode

    private val runner = QuestRunner()
    val currentQuest: Cell<QuestModel?> = _currentQuest
    val currentArea: Cell<AreaModel?> = _currentArea
    val showSectionIds: Cell<Boolean> = _showSectionIds
    val spawnMonstersOnGround: Cell<Boolean> = _spawnMonstersOnGround
    val omnispawn: Cell<Boolean> = _omnispawn
    val showOriginPoint: Cell<Boolean> = _showOriginPoint
    val currentAreaVariant: Cell<AreaVariantModel?> = _currentAreaVariant

    val currentAreaEvents: ListCell<QuestEventModel> =
        flatMapToList(currentQuest, currentArea) { quest, area ->
            if (quest != null && area != null) {
                if (quest.floorMappings.isNotEmpty()) {
                    // For quests with floor mappings, find events by floor IDs that map to this area
                    val relevantFloorIds = quest.floorMappings
                        .filter { it.areaId == area.id }
                        .map { it.floorId }
                        .toSet()
                    quest.events.filtered { event -> event.areaId in relevantFloorIds }
                } else {
                    // For regular quests, use direct area ID matching
                    quest.events.filtered { it.areaId == area.id }
                }
            } else {
                emptyListCell()
            }
        }

    val selectedEvent: Cell<QuestEventModel?> = _selectedEvent
    val selectedEvents: Cell<Set<QuestEventModel>> = _selectedEvents

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
            if (areaVariant != null) {
                // If we have an area variant, try to load sections regardless of quest status
                val episode = quest?.episode ?: Episode.I // Default to Episode I if no quest
                areaStore.getLoadedSections(episode, areaVariant) ?: emptyList()
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

    val showCollisionGeometry: Cell<Boolean> = _showCollisionGeometry
    val mouseWorldPosition: Cell<Vector3?> = _mouseWorldPosition
    val targetCameraPosition: Cell<Vector3?> = _targetCameraPosition
    val selectedSection: Cell<SectionModel?> = _selectedSection

    init {
        addDisposables(
            uiStore.onGlobalKeyDown(PwToolType.QuestEditor, "Ctrl-Alt-Shift-D") {
                _devMode.value = !_devMode.value

                logger.info { "Dev mode ${if (devMode.value) "on" else "off"}." }
            },
        )

        observeNow(uiStore.currentTool) { tool ->
            if (tool == PwToolType.QuestEditor) {
                makeMainUndoCurrent()
            }
        }


        if (initializeNewQuest) {
            scope.launch { setCurrentQuest(getDefaultQuest(Episode.I)) }
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

        _highlightedEntity.value = null
        _selectedEntity.value = null
        _selectedEvent.value = null
        _selectedSection.value = null

        if (quest == null) {
            _currentArea.value = null
            _currentAreaVariant.value = null
            _currentQuest.value = null
        } else {
            _currentQuest.value = quest

            // Set the appropriate area and variant based on quest type
            val firstAreaVariant = if (quest.floorMappings.isNotEmpty()) {
                // For quests with floor mappings, find the mapping for floor 0 (starting area)
                val floor0Mapping = quest.floorMappings.find { it.floorId == 0 }
                if (floor0Mapping != null) {
                    // Set currentArea to match the floor 0 mapping
                    _currentArea.value = areaStore.getArea(quest.episode, floor0Mapping.areaId)
                    // Return the corresponding variant
                    areaStore.getVariant(quest.episode, floor0Mapping.areaId, floor0Mapping.variantId)
                } else {
                    // Fallback: if no floor 0 mapping, find the first area variant with entities
                    val areaWithEntities = quest.areaVariants.value.find { variant ->
                        val hasNpcs = quest.npcs.value.any { it.areaId == variant.area.id }
                        val hasObjects = quest.objects.value.any { it.areaId == variant.area.id }
                        hasNpcs || hasObjects
                    }

                    if (areaWithEntities != null) {
                        _currentArea.value = areaWithEntities.area
                        areaWithEntities
                    } else {
                        quest.areaVariants.value.firstOrNull()?.also { variant ->
                            _currentArea.value = variant.area
                        }
                    }
                }
            } else {
                // For regular quests, use area 0 with variant 0
                _currentArea.value = areaStore.getArea(quest.episode, 0)
                areaStore.getArea(quest.episode, 0)?.areaVariants?.getOrNull(0)
            }

            _currentAreaVariant.value = firstAreaVariant

            // Load section data.
            updateQuestEntitySections(quest)

            // Ensure all entities have their section initialized.
            quest.npcs.value.forEach(QuestNpcModel::setSectionInitialized)
            quest.objects.value.forEach(QuestObjectModel::setSectionInitialized)

            // Trigger section loading for dropdown immediately after quest is loaded
            _sectionsUpdated.value += 1
        }
    }

    suspend fun getDefaultQuest(episode: Episode): QuestModel =
        convertQuestToModel(questLoader.loadDefaultQuest(episode), areaStore::getVariant)

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
        
        // Load sections for the new area if quest is loaded
        // Use setTimeout to ensure currentAreaVariant has been updated first
        window.setTimeout({
            currentQuest.value?.let { quest ->
                currentAreaVariant.value?.let { areaVariant ->
                    requestSectionLoading(quest.episode, areaVariant)
                }
            }
        }, 50)
    }

    fun setCurrentAreaVariant(variant: AreaVariantModel?) {
        _currentAreaVariant.value = variant
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
        // Simple implementation - just set the selected event
        _selectedEvent.value = event

        // Update multi-selection to match single selection
        if (event != null) {
            _selectedEvents.value = setOf(event)
        } else {
            _selectedEvents.value = emptySet()
        }
    }

    /**
     * Toggle event selection for multi-selection with Ctrl+click
     */
    fun toggleEventSelection(event: QuestEventModel) {
        val currentSelection = _selectedEvents.value.toMutableSet()

        if (event in currentSelection) {
            currentSelection.remove(event)
        } else {
            currentSelection.add(event)
        }

        _selectedEvents.value = currentSelection

        // Update single selection to the last selected event (or null if none)
        _selectedEvent.value = if (currentSelection.isEmpty()) null else event
    }

    fun <T> setEventProperty(
        event: QuestEventModel,
        setter: (QuestEventModel, T) -> Unit,
        value: T,
    ) {
        mutate {
            setSelectedEvent(event)
            setter(event, value)
        }
    }

    fun addEventAction(event: QuestEventModel, action: QuestEventActionModel) {
        mutate {
            setSelectedEvent(event)
            event.addAction(action)
        }
    }

    fun addEventAction(event: QuestEventModel, index: Int, action: QuestEventActionModel) {
        mutate {
            setSelectedEvent(event)
            event.addAction(index, action)
        }
    }

    fun removeEventAction(event: QuestEventModel, action: QuestEventActionModel) {
        mutate {
            setSelectedEvent(event)
            event.removeAction(action)
        }
    }

    fun <Action : QuestEventActionModel, T> setEventActionProperty(
        event: QuestEventModel,
        action: Action,
        setter: (Action, T) -> Unit,
        value: T,
    ) {
        mutate {
            setSelectedEvent(event)
            setter(action, value)
        }
    }

    fun setHighlightedEntity(entity: QuestEntityModel<*, *>?) {
        _highlightedEntity.value = entity
    }

    fun setSelectedEntity(entity: QuestEntityModel<*, *>?) {
        entity?.let {
            currentQuest.value?.let { quest ->
                // For quests with floor mappings, use the mapping to determine correct area and variant
                if (quest.floorMappings.isNotEmpty()) {
                    // Find the floor mapping for this entity's areaId (which is actually floorId in this context)
                    val floorMapping = quest.floorMappings.find { mapping -> mapping.floorId == entity.areaId }

                    if (floorMapping != null) {
                        // Use the floor mapping to set BOTH area and variant correctly
                        val newArea = areaStore.getArea(quest.episode, floorMapping.areaId)
                        val newVariant = areaStore.getVariant(quest.episode, floorMapping.areaId, floorMapping.variantId)

                        _currentArea.value = newArea
                        _currentAreaVariant.value = newVariant
                    } else {
                        // Fallback to entity's area
                        val newArea = areaStore.getArea(quest.episode, entity.areaId)
                        _currentArea.value = newArea
                    }
                } else {
                    // For regular quests, use entity's area directly
                    val newArea = areaStore.getArea(quest.episode, entity.areaId)
                    _currentArea.value = newArea
                }
            }
        }

        _selectedEntity.value = entity
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

    suspend fun setMapDesignations(mapDesignations: Map<Int, Set<Int>>) {
        currentQuest.value?.let { quest ->
            quest.setMapDesignations(mapDesignations)
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
            // Find all variants for this area and try to find the section in any of them
            val variants = quest.areaVariants.value.filter { it.area.id == entity.areaId }

            for (variant in variants) {
                val section = areaStore.getLoadedSections(quest.episode, variant)
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

    fun setShowCollisionGeometry(show: Boolean) {
        _showCollisionGeometry.value = show
    }

    fun setShowSectionIds(show: Boolean) {
        _showSectionIds.value = show
    }

    fun setSpawnMonstersOnGround(spawn: Boolean) {
        _spawnMonstersOnGround.value = spawn
        QuestNpcModel.setSpawnOnGround(spawn)
    }

    fun setOmnispawn(omnispawn: Boolean) {
        _omnispawn.value = omnispawn
    }

    fun setShowOriginPoint(show: Boolean) {
        _showOriginPoint.value = show
    }

    fun setMouseWorldPosition(position: Vector3?) {
        _mouseWorldPosition.value = position
    }

    fun setTargetCameraPosition(position: Vector3?) {
        _targetCameraPosition.value = position
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
                areaStore.getSections(episode, areaVariant)
                // Trigger UI update by incrementing the counter
                _sectionsUpdated.value += 1
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
                _targetCameraPosition.value = section.position.clone()
            }
        }
    }

    /**
     * Navigate camera to the section of a specific event.
     */
    fun goToEventSection(event: QuestEventModel) {
        goToSection(event.sectionId.value)
    }

    private suspend fun updateQuestEntitySections(quest: QuestModel) {
        quest.areaVariants.value.forEach { variant ->
            val sections = areaStore.getSections(quest.episode, variant)
            variant.setSections(sections)
            setSectionOnQuestEntities(quest.npcs.value, variant, sections)
            setSectionOnQuestEntities(quest.objects.value, variant, sections)
        }
    }

    private fun setSectionOnQuestEntities(
        entities: List<QuestEntityModel<*, *>>,
        variant: AreaVariantModel,
        sections: List<SectionModel>,
    ) {
        val quest = currentQuest.value ?: return

        entities.forEach { entity ->
            val shouldProcessEntity = if (quest.floorMappings.isNotEmpty()) {
                // For bb_map_designate quests, check if this entity's floor ID corresponds to this variant
                val floorMapping = quest.floorMappings.find { it.floorId == entity.areaId }
                floorMapping?.areaId == variant.area.id && floorMapping.variantId == variant.id
            } else {
                // For regular quests, use the original logic
                entity.areaId == variant.area.id
            }

            if (shouldProcessEntity) {
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
