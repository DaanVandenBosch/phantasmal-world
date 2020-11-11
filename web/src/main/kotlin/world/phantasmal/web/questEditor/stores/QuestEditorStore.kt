package world.phantasmal.web.questEditor.stores

import kotlinx.coroutines.CoroutineScope
import mu.KotlinLogging
import world.phantasmal.observable.value.*
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.core.actions.Action
import world.phantasmal.web.core.undo.UndoManager
import world.phantasmal.web.core.undo.UndoStack
import world.phantasmal.web.externals.babylon.Vector3
import world.phantasmal.web.questEditor.QuestRunner
import world.phantasmal.web.questEditor.actions.TranslateEntityAction
import world.phantasmal.web.questEditor.models.*
import world.phantasmal.webui.stores.Store

private val logger = KotlinLogging.logger {}

class QuestEditorStore(
    scope: CoroutineScope,
    private val uiStore: UiStore,
    private val areaStore: AreaStore,
) : Store(scope) {
    private val _currentQuest = mutableVal<QuestModel?>(null)
    private val _currentArea = mutableVal<AreaModel?>(null)
    private val _selectedWave = mutableVal<WaveModel?>(null)
    private val _selectedEntity = mutableVal<QuestEntityModel<*, *>?>(null)

    private val undoManager = UndoManager()
    private val mainUndo = UndoStack(undoManager)

    val runner = QuestRunner()
    val currentQuest: Val<QuestModel?> = _currentQuest
    val currentArea: Val<AreaModel?> = _currentArea
    val selectedWave: Val<WaveModel?> = _selectedWave
    val selectedEntity: Val<QuestEntityModel<*, *>?> = _selectedEntity

    val questEditingEnabled: Val<Boolean> = currentQuest.isNotNull() and !runner.running
    val canUndo: Val<Boolean> = questEditingEnabled and undoManager.canUndo
    val firstUndo: Val<Action?> = undoManager.firstUndo
    val canRedo: Val<Boolean> = questEditingEnabled and undoManager.canRedo
    val firstRedo: Val<Action?> = undoManager.firstRedo

    init {
        observe(uiStore.currentTool) { tool ->
            if (tool == PwToolType.QuestEditor) {
                mainUndo.makeCurrent()
            }
        }
    }

    fun makeMainUndoCurrent() {
        mainUndo.makeCurrent()
    }

    fun undo() {
        require(canUndo.value) { "Can't undo at the moment." }
        undoManager.undo()
    }

    fun redo() {
        require(canRedo.value) { "Can't redo at the moment." }
        undoManager.redo()
    }

    suspend fun setCurrentQuest(quest: QuestModel?) {
        mainUndo.reset()

        // TODO: Stop runner.

        _selectedEntity.value = null
        _selectedWave.value = null

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
                    entity.setSection(section)
                }
            }
        }
    }

    fun setCurrentArea(area: AreaModel?) {
        // TODO: Set wave.

        _selectedEntity.value = null
        _currentArea.value = area
    }

    fun setSelectedEntity(entity: QuestEntityModel<*, *>?) {
        entity?.let {
            currentQuest.value?.let { quest ->
                _currentArea.value = areaStore.getArea(quest.episode, entity.areaId)
            }
        }

        _selectedEntity.value = entity
    }

    fun translateEntity(
        entity: QuestEntityModel<*, *>,
        oldSection: SectionModel?,
        newSection: SectionModel?,
        oldPosition: Vector3,
        newPosition: Vector3,
        world: Boolean,
    ) {
        mainUndo.push(TranslateEntityAction(
            ::setSelectedEntity,
            entity,
            oldSection,
            newSection,
            oldPosition,
            newPosition,
            world,
        )).execute()
    }
}
