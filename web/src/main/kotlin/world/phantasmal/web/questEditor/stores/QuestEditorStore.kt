package world.phantasmal.web.questEditor.stores

import kotlinx.coroutines.launch
import mu.KotlinLogging
import world.phantasmal.lib.Episode
import world.phantasmal.observable.cell.*
import world.phantasmal.observable.cell.list.emptyListCell
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.actions.Action
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.core.undo.UndoManager
import world.phantasmal.web.core.undo.UndoStack
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
) : Store() {
    private val _devMode = mutableCell(false)
    private val _currentQuest = mutableCell<QuestModel?>(null)
    private val _currentArea = mutableCell<AreaModel?>(null)
    private val _selectedEvent = mutableCell<QuestEventModel?>(null)
    private val _highlightedEntity = mutableCell<QuestEntityModel<*, *>?>(null)
    private val _selectedEntity = mutableCell<QuestEntityModel<*, *>?>(null)
    private val mainUndo = UndoStack(undoManager)
    private val _showCollisionGeometry = mutableCell(true)

    val devMode: Cell<Boolean> = _devMode

    private val runner = QuestRunner()
    val currentQuest: Cell<QuestModel?> = _currentQuest
    val currentArea: Cell<AreaModel?> = _currentArea
    val currentAreaVariant: Cell<AreaVariantModel?> =
        map(currentArea, currentQuest.flatMapNull { it?.areaVariants }) { area, variants ->
            if (area != null && variants != null) {
                variants.find { it.area.id == area.id } ?: area.areaVariants.first()
            } else {
                null
            }
        }
    val selectedEvent: Cell<QuestEventModel?> = _selectedEvent

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
    val firstUndo: Cell<Action?> = undoManager.firstUndo
    val canRedo: Cell<Boolean> = questEditingEnabled and undoManager.canRedo
    val firstRedo: Cell<Action?> = undoManager.firstRedo

    /**
     * True if there have been changes since the last save.
     */
    val canSaveChanges: Cell<Boolean> = !undoManager.allAtSavePoint

    val showCollisionGeometry: Cell<Boolean> = _showCollisionGeometry

    init {
        addDisposables(
            uiStore.onGlobalKeyDown(PwToolType.QuestEditor, "Ctrl-Alt-Shift-D") {
                _devMode.value = !_devMode.value

                logger.info { "Dev mode ${if (devMode.value) "on" else "off"}." }
            },
        )

        observe(uiStore.currentTool) { tool ->
            if (tool == PwToolType.QuestEditor) {
                makeMainUndoCurrent()
            }
        }

        observe(currentQuest.flatMap { it?.npcs ?: emptyListCell() }) { npcs ->
            val selected = selectedEntity.value

            if (selected is QuestNpcModel && selected !in npcs) {
                _selectedEntity.value = null
            }
        }

        observe(currentQuest.flatMap { it?.objects ?: emptyListCell() }) { objects ->
            val selected = selectedEntity.value

            if (selected is QuestObjectModel && selected !in objects) {
                _selectedEntity.value = null
            }
        }

        scope.launch { setCurrentQuest(getDefaultQuest(Episode.I)) }
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

        if (quest == null) {
            _currentArea.value = null
            _currentQuest.value = null
        } else {
            _currentArea.value = areaStore.getArea(quest.episode, 0)
            _currentQuest.value = quest

            // Load section data.
            updateQuestEntitySections(quest)

            // Ensure all entities have their section initialized.
            quest.npcs.value.forEach { it.setSectionInitialized() }
            quest.objects.value.forEach { it.setSectionInitialized() }
        }
    }

    suspend fun getDefaultQuest(episode: Episode): QuestModel =
        convertQuestToModel(questLoader.loadDefaultQuest(episode), areaStore::getVariant)

    fun setCurrentArea(area: AreaModel?) {
        val event = selectedEvent.value

        if (area != null && event != null && area.id != event.areaId) {
            setSelectedEvent(null)
        }

        _highlightedEntity.value = null
        _selectedEntity.value = null
        _currentArea.value = area
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
        }

        _selectedEvent.value = event
    }

    fun setHighlightedEntity(entity: QuestEntityModel<*, *>?) {
        _highlightedEntity.value = entity
    }

    fun setSelectedEntity(entity: QuestEntityModel<*, *>?) {
        entity?.let {
            currentQuest.value?.let { quest ->
                _currentArea.value = areaStore.getArea(quest.episode, entity.areaId)
            }
        }

        _selectedEntity.value = entity
    }

    suspend fun setMapDesignations(mapDesignations: Map<Int, Int>) {
        currentQuest.value?.let { quest ->
            quest.setMapDesignations(mapDesignations)
            updateQuestEntitySections(quest)
        }
    }

    fun setEntitySection(entity: QuestEntityModel<*, *>, sectionId: Int) {
        currentQuest.value?.let { quest ->
            val variant = quest.areaVariants.value.find { it.area.id == entity.areaId }

            variant?.let {
                val section = areaStore.getLoadedSections(quest.episode, variant)
                    ?.find { it.id == sectionId }

                if (section == null) {
                    entity.setSectionId(sectionId)
                } else {
                    entity.setSection(section)
                }
            }
        }
    }

    fun executeAction(action: Action) {
        pushAction(action)
        action.execute()
    }

    fun pushAction(action: Action) {
        require(questEditingEnabled.value) {
            val reason = when {
                currentQuest.value == null -> " (no current quest)"
                runner.running.value -> " (QuestRunner is running)"
                else -> ""
            }
            "Quest editing is disabled at the moment$reason."
        }
        mainUndo.push(action)
    }

    fun setShowCollisionGeometry(show: Boolean) {
        _showCollisionGeometry.value = show
    }

    fun questSaved() {
        undoManager.savePoint()
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
        entities.forEach { entity ->
            if (entity.areaId == variant.area.id) {
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
