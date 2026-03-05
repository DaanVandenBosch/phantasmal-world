package world.phantasmal.web.questEditor.rendering

import world.phantasmal.cell.and
import world.phantasmal.cell.eq
import world.phantasmal.cell.flatMapNull
import world.phantasmal.cell.list.emptyListCell
import world.phantasmal.cell.list.filteredCell
import world.phantasmal.cell.or
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.stores.QuestEditorStore

class QuestEditorMeshManager(
    areaAssetLoader: AreaAssetLoader,
    entityAssetLoader: EntityAssetLoader,
    questEditorStore: QuestEditorStore,
    renderContext: QuestRenderContext,
) : QuestMeshManager(areaAssetLoader, entityAssetLoader, questEditorStore, renderContext) {
    init {
        observeNow(
            questEditorStore.currentQuest,
            questEditorStore.currentAreaVariant,
        ) { quest, areaVariant ->
            loadAreaMeshes(quest?.episode, areaVariant)
        }

        observeNow(
            questEditorStore.currentQuest,
            questEditorStore.currentArea,
            questEditorStore.currentAreaVariant,
            questEditorStore.selectedEvent.flatMapNull { it?.wave },
        ) { quest, area, areaVariant, wave ->
            loadNpcMeshes(
                if (quest != null && area != null) {
                    // For multi-floor quests, find floor IDs that map to this area+variant
                    val relevantFloorIds = if (quest.floorMappings.isNotEmpty() && areaVariant != null) {
                        quest.floorMappings
                            .filter { it.areaId == area.id && it.variantId == areaVariant.id }
                            .map { it.floorId }
                            .toSet()
                    } else {
                        null
                    }

                    quest.npcs.filteredCell {
                        val areaMatch = if (relevantFloorIds != null) {
                            it.areaId in relevantFloorIds
                        } else {
                            it.areaId == area.id
                        }
                        it.sectionInitialized and areaMatch and
                                ((wave == null) or (it.wave eq wave))
                    }
                } else {
                    emptyListCell()
                }
            )
        }

        observeNow(
            questEditorStore.currentQuest,
            questEditorStore.currentArea,
            questEditorStore.currentAreaVariant,
        ) { quest, area, areaVariant ->
            loadObjectMeshes(
                if (quest != null && area != null) {
                    val relevantFloorIds = if (quest.floorMappings.isNotEmpty() && areaVariant != null) {
                        quest.floorMappings
                            .filter { it.areaId == area.id && it.variantId == areaVariant.id }
                            .map { it.floorId }
                            .toSet()
                    } else {
                        null
                    }

                    quest.objects.filteredCell {
                        val areaMatch = if (relevantFloorIds != null) {
                            it.areaId in relevantFloorIds
                        } else {
                            it.areaId == area.id
                        }
                        it.sectionInitialized and areaMatch
                    }
                } else {
                    emptyListCell()
                }
            )
        }

        observeNow(questEditorStore.showCollisionGeometry) {
            renderContext.collisionGeometryVisible = it
            renderContext.renderGeometryVisible = !it
        }
    }
}
