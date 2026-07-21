package world.phantasmal.web.questEditor.rendering

import world.phantasmal.cell.Cell
import world.phantasmal.cell.and
import world.phantasmal.cell.cell
import world.phantasmal.cell.flatMap
import world.phantasmal.cell.list.emptyListCell
import world.phantasmal.cell.list.filteredCell
import world.phantasmal.psolib.asm.dataFlowAnalysis.ParticleSpawn
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.fileFormats.particle.GLOBAL_PARTICLE_EFFECT_COUNT
import world.phantasmal.web.questEditor.asm.SymbolChatTriggerInfo
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.loading.ParticleAssetLoader
import world.phantasmal.web.questEditor.loading.SymbolChatColliRepository
import world.phantasmal.web.questEditor.stores.*

internal fun particleTemplateMapIds(
    spawn: ParticleSpawn,
    floorToMapId: Map<Int, Int>,
    visibleFloorIds: Set<Int>,
    currentMapId: Int,
): Set<Int> = if (spawn.particleId < GLOBAL_PARTICLE_EFFECT_COUNT) {
    setOf(currentMapId)
} else {
    spawn.executionFloorIds.asSequence()
        .filter { it in visibleFloorIds }
        .mapNotNullTo(mutableSetOf()) { floorToMapId[it] }
        .ifEmpty { setOf(currentMapId) }
}

/**
 * Selects fixed quest particle emitters for one editor floor view.
 *
 * A DAT emitter belongs to its entity floor; an opcode emitter belongs to its execution paths.
 * An unresolved opcode is not shown in an arbitrary floor because the client clears the
 * particle system during floor transitions; treating unknown as global would be false.
 */
internal fun particleSpawnsForFloorView(
    spawns: List<ParticleSpawn>,
    visibleFloorIds: Set<Int>,
): List<ParticleSpawn> = spawns.filter { spawn ->
    spawn.executionFloorIds.any { it in visibleFloorIds }
}

class QuestEditorMeshManager(
    areaAssetLoader: AreaAssetLoader,
    entityAssetLoader: EntityAssetLoader,
    particleAssetLoader: ParticleAssetLoader,
    questEditorStore: QuestEditorStore,
    questEditorUiStore: QuestEditorUiStore,
    playbackVisualizationStore: PlaybackVisualizationStore,
    viewportStore: ViewportStore,
    areaStore: AreaStore,
    renderContext: QuestRenderContext,
    symbolChatColliRepository: SymbolChatColliRepository,
    symbolChatTriggers: Cell<List<SymbolChatTriggerInfo>>,
    readSegmentData: (Int) -> Buffer?,
) : QuestMeshManager(areaAssetLoader, entityAssetLoader, particleAssetLoader, questEditorStore, questEditorUiStore, playbackVisualizationStore, areaStore, renderContext) {
    private val questParticleSpawns = questEditorStore.currentQuest.flatMap { quest ->
        quest?.particleSpawns ?: cell(emptyList())
    }

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
    private val gotoIndicatorManager = addDisposable(GotoIndicatorManager(renderContext))

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

        observeNow(
            questEditorStore.currentQuest,
            questEditorStore.currentArea,
            questEditorStore.currentFloorIds,
            questEditorUiStore.showQuestParticles,
            questParticleSpawns,
        ) { quest, area, floorIds, showQuestParticles, particleSpawns ->
            if (quest == null || area == null || !showQuestParticles) {
                loadParticleMarkers(emptyList())
            } else {
                // The runtime emitter has no floor field. DAT objects supply an exact floor;
                // opcode emitters are scoped by the script path that creates them. An unresolved
                // opcode is not assigned to an arbitrary floor.
                val visibleFloorIds = floorIds ?: setOf(area.id)
                val mapId = quest.floorMappings.firstOrNull { mapping ->
                    if (floorIds != null) mapping.floorId in floorIds else mapping.areaId == area.id
                }?.mapId ?: area.id
                val floorToMapId = quest.floorMappings.associate { it.floorId to it.mapId }
                loadParticleMarkers(
                    spawns = particleSpawnsForFloorView(particleSpawns, visibleFloorIds),
                    // A map-local template is selected when the emitter is created. Its source
                    // floors recover the corresponding map-specific resource table.
                    resolveTemplateMapIds = {
                        particleTemplateMapIds(it, floorToMapId, visibleFloorIds, mapId)
                    },
                    resolveEntityPosition = { entityId ->
                        when (entityId) {
                            in 0x1000..0x3FFF -> quest.npcs.value
                                .firstOrNull {
                                    it.areaId in visibleFloorIds &&
                                            (it.entity.id + 0x1000) and 0xFFFF == entityId
                                }
                                ?.worldPosition?.value
                            in 0x4000..0xFFFF -> quest.objects.value
                                .firstOrNull {
                                    it.areaId in visibleFloorIds &&
                                            (it.entity.id.toInt() + 0x4000) and 0xFFFF == entityId
                                }
                                ?.worldPosition?.value
                            // Player IDs 0..11 have no fixed position in a quest file.
                            else -> null
                        }
                    },
                )
            }
        }

        observeNow(questEditorUiStore.showCollisionGeometry) {
            renderContext.collisionGeometryVisible = it
            renderContext.renderGeometryVisible = !it
        }

        observeNow(viewportStore.gotoIndicatorPosition) { pos ->
            gotoIndicatorManager.setPosition(pos)
        }
    }

    override fun beforeRender() {
        super.beforeRender()
        symbolChatBillboardManager.beforeRender()
        gotoIndicatorManager.update()
    }

}
