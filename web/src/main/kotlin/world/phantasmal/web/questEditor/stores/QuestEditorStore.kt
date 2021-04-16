package world.phantasmal.web.questEditor.stores

import kotlinx.coroutines.launch
import mu.KotlinLogging
import world.phantasmal.lib.Episode
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.and
import world.phantasmal.observable.value.list.emptyListVal
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.observable.value.not
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
    private val _devMode = mutableVal(false)
    private val _currentQuest = mutableVal<QuestModel?>(null)
    private val _currentArea = mutableVal<AreaModel?>(null)
    private val _selectedEvent = mutableVal<QuestEventModel?>(null)
    private val _highlightedEntity = mutableVal<QuestEntityModel<*, *>?>(null)
    private val _selectedEntity = mutableVal<QuestEntityModel<*, *>?>(null)
    private val mainUndo = UndoStack(undoManager)
    private val _showCollisionGeometry = mutableVal(true)

    val devMode: Val<Boolean> = _devMode

    val runner = QuestRunner()
    val currentQuest: Val<QuestModel?> = _currentQuest
    val currentArea: Val<AreaModel?> = _currentArea
    val selectedEvent: Val<QuestEventModel?> = _selectedEvent

    /**
     * The entity the user is currently hovering over.
     */
    val highlightedEntity: Val<QuestEntityModel<*, *>?> = _highlightedEntity

    /**
     * The entity the user has selected, typically by clicking it.
     */
    val selectedEntity: Val<QuestEntityModel<*, *>?> = _selectedEntity

    val questEditingEnabled: Val<Boolean> = currentQuest.isNotNull() and !runner.running

    val canUndo: Val<Boolean> = questEditingEnabled and undoManager.canUndo
    val firstUndo: Val<Action?> = undoManager.firstUndo
    val canRedo: Val<Boolean> = questEditingEnabled and undoManager.canRedo
    val firstRedo: Val<Action?> = undoManager.firstRedo

    /**
     * True if there have been changes since the last save.
     */
    val canSaveChanges: Val<Boolean> = !undoManager.allAtSavePoint

    val showCollisionGeometry: Val<Boolean> = _showCollisionGeometry

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

        observe(currentQuest.flatMap { it?.npcs ?: emptyListVal() }) { npcs ->
            val selected = selectedEntity.value

            if (selected is QuestNpcModel && selected !in npcs) {
                _selectedEntity.value = null
            }
        }

        observe(currentQuest.flatMap { it?.objects ?: emptyListVal() }) { objects ->
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
            quest.areaVariants.value.forEach { variant ->
                val sections = areaStore.getSections(quest.episode, variant)
                variant.setSections(sections)
                setSectionOnQuestEntities(quest.npcs.value, variant, sections)
                setSectionOnQuestEntities(quest.objects.value, variant, sections)
            }

            // Ensure all entities have their section initialized.
            quest.npcs.value.forEach { it.setSectionInitialized() }
            quest.objects.value.forEach { it.setSectionInitialized() }
        }
    }

    suspend fun getDefaultQuest(episode: Episode): QuestModel =
        convertQuestToModel(questLoader.loadDefaultQuest(episode), areaStore::getVariant)

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
                    entity.initializeSection(section)
                }
            }
        }
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
}
