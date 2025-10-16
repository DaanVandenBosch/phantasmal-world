package world.phantasmal.web.questEditor.rendering

import world.phantasmal.cell.and
import world.phantasmal.cell.list.emptyListCell
import world.phantasmal.cell.list.filteredCell
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.stores.AreaStore
import world.phantasmal.web.questEditor.stores.QuestEditorStore

class QuestEditorMeshManager(
    areaAssetLoader: AreaAssetLoader,
    entityAssetLoader: EntityAssetLoader,
    questEditorStore: QuestEditorStore,
    areaStore: AreaStore,
    renderContext: QuestRenderContext,
) : QuestMeshManager(areaAssetLoader, entityAssetLoader, questEditorStore, areaStore, renderContext) {
    init {
        observeNow(
            questEditorStore.currentQuest,
            questEditorStore.currentArea,
            questEditorStore.currentAreaVariant,
        ) { quest, area, areaVariant ->
            if (quest != null && area != null) {
                if (areaVariant != null) {
                    // Load the specific variant
                    loadAreaMeshes(quest.episode, areaVariant)
                } else {
                    // For areas without variants (like Lab, Pioneer2), load the default variant
                    val defaultVariant = area.areaVariants.firstOrNull()
                    loadAreaMeshes(quest.episode, defaultVariant)
                }
            } else {
                loadAreaMeshes(null, null)
            }
        }

        observeNow(
            questEditorStore.currentQuest,
            questEditorStore.currentArea,
            questEditorStore.currentAreaVariant,
            questEditorStore.selectedEventsSectionWaves,
        ) { quest, area, areaVariant, selectedSectionWaves ->
            loadNpcMeshes(
                if (quest != null && area != null) {
                    quest.npcs.filteredCell {
                        val entityBelongsToCurrentAreaVariant =
                            if (quest.floorMappings.isNotEmpty() && areaVariant != null) {
                                val floorMapping = quest.floorMappings.find { mapping -> mapping.floorId == it.areaId }
                                floorMapping?.areaId == area.id && floorMapping.variantId == areaVariant.id
                            } else {
                                it.areaId == area.id
                            }

                        val sectionInit = it.sectionInitialized
                        val belongsToArea = entityBelongsToCurrentAreaVariant

                        // Only show NPCs that match the exact section and wave combination of selected events
                        val matchesSelectedEvent = if (selectedSectionWaves.isEmpty()) {
                            true // Show all NPCs when no events are selected
                        } else {
                            selectedSectionWaves.contains(Pair(it.sectionId.value, it.wave.value.id))
                        }

                        val shouldShow = sectionInit and belongsToArea and matchesSelectedEvent

                        shouldShow
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
                    quest.objects.filteredCell {
                        val entityBelongsToCurrentAreaVariant =
                            if (quest.floorMappings.isNotEmpty() && areaVariant != null) {
                                val floorMapping = quest.floorMappings.find { mapping -> mapping.floorId == it.areaId }
                                floorMapping?.areaId == area.id && floorMapping.variantId == areaVariant.id
                            } else {
                                it.areaId == area.id
                            }

                        it.sectionInitialized and entityBelongsToCurrentAreaVariant
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
