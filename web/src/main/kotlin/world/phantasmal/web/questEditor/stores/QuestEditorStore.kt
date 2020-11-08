package world.phantasmal.web.questEditor.stores

import kotlinx.coroutines.CoroutineScope
import mu.KotlinLogging
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.web.questEditor.models.*
import world.phantasmal.webui.stores.Store

private val logger = KotlinLogging.logger {}

class QuestEditorStore(scope: CoroutineScope, private val areaStore: AreaStore) : Store(scope) {
    private val _currentQuest = mutableVal<QuestModel?>(null)
    private val _currentArea = mutableVal<AreaModel?>(null)
    private val _selectedWave = mutableVal<WaveModel?>(null)
    private val _selectedEntity = mutableVal<QuestEntityModel<*, *>?>(null)

    val currentQuest: Val<QuestModel?> = _currentQuest
    val currentArea: Val<AreaModel?> = _currentArea
    val selectedWave: Val<WaveModel?> = _selectedWave
    val selectedEntity: Val<QuestEntityModel<*, *>?> = _selectedEntity

    // TODO: Take into account whether we're debugging or not.
    val questEditingDisabled: Val<Boolean> = currentQuest.map { it == null }

    suspend fun setCurrentQuest(quest: QuestModel?) {
        _currentArea.value = null
        _currentQuest.value = quest

        quest?.let {
            _currentArea.value = areaStore.getArea(quest.episode, 0)

            // Load section data.
            quest.areaVariants.value.forEach { variant ->
                val sections = areaStore.getSections(quest.episode, variant)
                variant.setSections(sections)
                setSectionOnQuestEntities(quest.npcs.value, variant, sections)
                setSectionOnQuestEntities(quest.objects.value, variant, sections)
            }
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
                } else {
                    entity.setSection(section)
                }
            }
        }
    }

    fun setSelectedEntity(entity: QuestEntityModel<*, *>?) {
        entity?.let {
            currentQuest.value?.let { quest ->
                _currentArea.value = areaStore.getArea(quest.episode, entity.areaId)
            }
        }

        _selectedEntity.value = entity
    }
}
