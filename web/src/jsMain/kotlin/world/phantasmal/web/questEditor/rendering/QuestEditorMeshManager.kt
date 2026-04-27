package world.phantasmal.web.questEditor.rendering

import world.phantasmal.cell.Cell
import world.phantasmal.cell.and
import world.phantasmal.cell.list.emptyListCell
import world.phantasmal.cell.list.filteredCell
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.web.questEditor.asm.SymbolChatTriggerInfo
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.loading.SymbolChatColliRepository
import world.phantasmal.web.questEditor.stores.*

class QuestEditorMeshManager(
    areaAssetLoader: AreaAssetLoader,
    entityAssetLoader: EntityAssetLoader,
    questEditorStore: QuestEditorStore,
    questEditorUiStore: QuestEditorUiStore,
    playbackVisualizationStore: PlaybackVisualizationStore,
    areaStore: AreaStore,
    renderContext: QuestRenderContext,
    symbolChatColliRepository: SymbolChatColliRepository,
    symbolChatTriggers: Cell<List<SymbolChatTriggerInfo>>,
    readSegmentData: (Int) -> Buffer?,
) : QuestMeshManager(areaAssetLoader, entityAssetLoader, questEditorStore, questEditorUiStore, playbackVisualizationStore, areaStore, renderContext) {
    private val symbolChatTriggerManager = addDisposable(
        SymbolChatTriggerManager(
            triggers = symbolChatTriggers,
            readSegmentData = readSegmentData,
            symbolChatColliRepository = symbolChatColliRepository,
            renderContext = renderContext,
        )
    )
    private val symbolChatBillboardManager = addDisposable(
        SymbolChatBillboardManager(questEditorStore, symbolChatColliRepository, renderContext)
    )

    init {
        observeNow(
            questEditorStore.currentQuest,
            questEditorStore.currentArea,
            questEditorStore.currentAreaVariant,
        ) { quest, area, areaVariant ->
            if (quest != null && area != null) {
                if (areaVariant != null) {
                    // Load the specific variant
                    // Use variant's episode to handle cross-episode maps (e.g., EP4 quest using EP2 Lab)
                    loadAreaMeshes(areaVariant.episode, areaVariant)
                } else {
                    // For areas without variants (like Lab, Pioneer2), load the default variant
                    val defaultVariant = area.areaVariants.firstOrNull()
                    loadAreaMeshes(defaultVariant?.episode ?: quest.episode, defaultVariant)
                }
            } else {
                loadAreaMeshes(null, null)
            }
        }

        observeNow(
            questEditorStore.currentQuest,
            questEditorStore.currentArea,
            questEditorStore.currentFloorIds,
            questEditorStore.selectedEventsSectionWaves,
        ) { quest, area, floorIds, selectedSectionWaves ->
            loadNpcMeshes(
                if (quest != null && area != null) {
                    quest.npcs.filteredCell {
                        val areaMatch = if (floorIds != null) {
                            it.areaId in floorIds
                        } else {
                            it.areaId == area.id
                        }

                        val matchesSelectedEvent = if (selectedSectionWaves.isEmpty()) {
                            true
                        } else {
                            selectedSectionWaves.contains(Pair(it.sectionId.value, it.wave.value.id))
                        }

                        it.sectionInitialized and areaMatch and matchesSelectedEvent
                    }
                } else {
                    emptyListCell()
                }
            )
        }

        observeNow(
            questEditorStore.currentQuest,
            questEditorStore.currentArea,
            questEditorStore.currentFloorIds,
        ) { quest, area, floorIds ->
            loadObjectMeshes(
                if (quest != null && area != null) {
                    quest.objects.filteredCell {
                        val areaMatch = if (floorIds != null) {
                            it.areaId in floorIds
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

        observeNow(questEditorUiStore.showCollisionGeometry) {
            renderContext.collisionGeometryVisible = it
            renderContext.renderGeometryVisible = !it
        }
    }

    override fun beforeRender() {
        super.beforeRender()
        symbolChatBillboardManager.beforeRender()
    }
}
