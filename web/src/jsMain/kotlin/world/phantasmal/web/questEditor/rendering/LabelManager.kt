package world.phantasmal.web.questEditor.rendering

import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import world.phantasmal.cell.map
import world.phantasmal.cell.observeNow
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.DisposableSupervisedScope
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.fileFormats.quest.ObjectType
import world.phantasmal.web.core.rendering.disposeObject3DResources
import world.phantasmal.web.externals.three.Group
import world.phantasmal.web.externals.three.Object3D
import world.phantasmal.web.questEditor.models.AreaVariantModel
import world.phantasmal.web.questEditor.models.QuestObjectModel
import world.phantasmal.web.questEditor.models.SectionModel
import world.phantasmal.web.questEditor.stores.AreaStore
import world.phantasmal.web.questEditor.stores.PlaybackVisualizationStore
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.web.questEditor.stores.QuestEditorUiStore
import world.phantasmal.webui.DisposableContainer

private val logger = KotlinLogging.logger {}

/**
 * Manages section ID labels, door ID labels, and playback spawn labels.
 */
class LabelManager(
    private val questEditorStore: QuestEditorStore,
    private val questEditorUiStore: QuestEditorUiStore,
    private val playbackVisualizationStore: PlaybackVisualizationStore,
    private val renderContext: QuestRenderContext,
    private val areaStore: AreaStore,
) : DisposableContainer() {
    private val scope = addDisposable(DisposableSupervisedScope(this::class, Dispatchers.Main))
    val sectionIdRenderer = SectionIdRenderer()

    private val sectionIdLabels = mutableMapOf<Int, Group>()
    private var sectionIdObserverDisposer = addDisposable(Disposer())
    private val doorIdLabels = mutableListOf<Group>()
    private val doorFlashTimers = mutableListOf<Int>()
    private val playbackLabels = mutableListOf<Group>()
    private var sectionsDisposer: Disposable? = null

    init {
        observeNow(questEditorStore.currentAreaVariant) { areaVariant ->
            sectionsDisposer?.dispose()
            sectionsDisposer = null

            updateSectionIdLabels()
            updateDoorIdLabels()

            if (areaVariant != null) {
                sectionsDisposer = areaVariant.sections.observeNow { _ ->
                    updateSectionIdLabels()
                    updateDoorIdLabels()
                }
            }
        }
        observeNow(questEditorUiStore.showSectionIds) { _ ->
            updateSectionIdLabels()
        }
        observeNow(questEditorUiStore.showDoorIds) { _ ->
            updateDoorIdLabels()
        }
        observeNow(questEditorStore.selectedSection) { _ ->
            updateSectionIdLabels()
        }

        observeNow(
            map(
                playbackVisualizationStore.playbackDoorIds,
                playbackVisualizationStore.playbackLockDoorIds,
                playbackVisualizationStore.playbackSpawnSectionIds,
            ) { unlock, lock, spawn -> Triple(unlock, lock, spawn) }
        ) { (_, _, spawnSectionIds) ->
            updateDoorIdLabels()
            updateSpawnLabels(spawnSectionIds)
        }
    }

    override fun dispose() {
        sectionsDisposer?.dispose()
        sectionsDisposer = null
        clearSectionIdLabels()
        clearDoorIdLabels()
        clearPlaybackLabels()
        super.dispose()
    }

    /**
     * Called before each render to update text scales for constant screen size.
     */
    fun beforeRender() {
        if (sectionIdLabels.isNotEmpty()) {
            sectionIdRenderer.updateTextScales(renderContext.camera, sectionIdLabels.values)
        }
        if (doorIdLabels.isNotEmpty()) {
            sectionIdRenderer.updateTextScales(renderContext.camera, doorIdLabels)
        }
        if (playbackLabels.isNotEmpty()) {
            sectionIdRenderer.updateTextScales(renderContext.camera, playbackLabels)
        }
    }

    // ---- Section ID labels ----

    private fun updateSectionIdLabels() {
        clearSectionIdLabels()

        if (!questEditorUiStore.showSectionIds.value) {
            return
        }

        val currentAreaVariant = questEditorStore.currentAreaVariant.value ?: return
        val currentQuest = questEditorStore.currentQuest.value
        val episode = currentQuest?.episode ?: Episode.I

        val sections = currentAreaVariant.sections.value
        if (sections.isNotEmpty()) {
            createSectionLabelsForSections(sections, currentAreaVariant)
        } else {
            scope.launch {
                try {
                    if (questEditorStore.currentAreaVariant.value != currentAreaVariant) {
                        return@launch
                    }

                    val loadedSections = areaStore.getSections(episode, currentAreaVariant)

                    if (questEditorStore.currentAreaVariant.value != currentAreaVariant) {
                        return@launch
                    }

                    currentAreaVariant.setSections(loadedSections)

                    if (loadedSections.isNotEmpty()) {
                        createSectionLabelsForSections(loadedSections, currentAreaVariant)
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to load sections for area variant ${currentAreaVariant.area.name}" }
                }
            }
        }
    }

    private fun createSectionLabelsForSections(
        sections: List<SectionModel>,
        areaVariant: AreaVariantModel,
    ) {
        for (section in sections) {
            val sectionCenter = section.position
            val uniqueKey = "${areaVariant.area.id}_${areaVariant.id}_${section.id}".hashCode()

            createSectionIdLabelForSection(
                uniqueKey,
                section.id,
                sectionCenter.x.toFloat(),
                sectionCenter.y.toFloat(),
                sectionCenter.z.toFloat()
            )
        }
    }

    private fun createSectionIdLabelForSection(
        uniqueKey: Int,
        sectionId: Int,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
    ) {
        val selectedSection = questEditorStore.selectedSection.value
        val isSelected = selectedSection?.id == sectionId

        val label = if (isSelected) {
            sectionIdRenderer.createSelectedSectionIdLabel(centerX, centerY, centerZ, sectionId)
        } else {
            sectionIdRenderer.createSectionIdLabel(centerX, centerY, centerZ, sectionId)
        }
        renderContext.helpers.add(label)
        sectionIdLabels[uniqueKey] = label
    }

    private fun clearSectionIdLabels() {
        for (label in sectionIdLabels.values) {
            renderContext.helpers.remove(label)
            renderContext.scene.remove(label)
            disposeObject3DResources(label)
        }
        sectionIdLabels.clear()

        val toRemove = mutableListOf<Object3D>()
        renderContext.helpers.children.forEach { child ->
            if (child.name.startsWith("SectionIdLabel_") || child.name.startsWith("SelectedSectionIdLabel_")) {
                toRemove.add(child)
            }
        }
        for (obj in toRemove) {
            renderContext.helpers.remove(obj)
            disposeObject3DResources(obj)
        }

        sectionIdObserverDisposer.disposeAll()
    }

    // ---- Door ID labels ----

    private fun updateDoorIdLabels() {
        clearDoorIdLabels()

        if (!questEditorUiStore.showDoorIds.value) return

        val quest = questEditorStore.currentQuest.value ?: return
        val area = questEditorStore.currentArea.value ?: return
        val areaVariant = questEditorStore.currentAreaVariant.value

        val unlockIds = playbackVisualizationStore.playbackDoorIds.value
        val lockIds = playbackVisualizationStore.playbackLockDoorIds.value

        for (obj in quest.objects.value) {
            if (!quest.entityBelongsToArea(obj.areaId, area.id, areaVariant?.id)) continue

            if (obj.type !in doorObjectTypes) continue
            val doorIdValue = getEffectiveDoorId(obj)
            if (doorIdValue < 0) continue

            val switchAmount = getSwitchAmount(obj)
            val doorIds = (doorIdValue until doorIdValue + switchAmount).toSet()
            val matchedUnlockIds = doorIds.intersect(unlockIds)
            val matchedLockIds = doorIds.intersect(lockIds)
            val isAction = matchedUnlockIds.isNotEmpty() || matchedLockIds.isNotEmpty()

            val isFence = obj.type in fenceObjectTypes
            val rangeStr =
                if (switchAmount > 1) "$doorIdValue-${doorIdValue + switchAmount - 1}"
                else "$doorIdValue"
            val defaultLabel = if (isFence) "Fence $rangeStr" else "Door $rangeStr"

            val (text, color) = when {
                matchedUnlockIds.isNotEmpty() -> {
                    val ids = matchedUnlockIds.sorted().joinToString(",")
                    "Unlock $ids" to SectionIdRenderer.UNLOCK_COLOR
                }
                matchedLockIds.isNotEmpty() -> {
                    val ids = matchedLockIds.sorted().joinToString(",")
                    "Lock $ids" to SectionIdRenderer.LOCK_COLOR
                }
                else -> defaultLabel to SectionIdRenderer.DOOR_ID_COLOR
            }

            val pos = obj.worldPosition.value
            val label = sectionIdRenderer.createDoorLabel(
                pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat(),
                text, color,
            )
            renderContext.helpers.add(label)
            doorIdLabels.add(label)

            if (isAction) {
                flashLabel(label)
            }
        }
    }

    private fun clearDoorIdLabels() {
        for (id in doorFlashTimers) window.clearTimeout(id)
        doorFlashTimers.clear()
        for (label in doorIdLabels) {
            renderContext.helpers.remove(label)
            disposeObject3DResources(label)
        }
        doorIdLabels.clear()
    }

    private fun flashLabel(label: Group) {
        val step = 150
        for (i in 1..4) {
            doorFlashTimers.add(window.setTimeout({
                label.visible = i % 2 == 0
            }, step * i))
        }
        doorFlashTimers.add(window.setTimeout({ label.visible = true }, step * 5))
    }

    // ---- Spawn labels ----

    private fun updateSpawnLabels(spawnSectionIds: Set<Int>) {
        clearPlaybackLabels()

        if (spawnSectionIds.isEmpty()) return

        val quest = questEditorStore.currentQuest.value ?: return
        val areaVariant = questEditorStore.currentAreaVariant.value

        val episode = quest.episode
        val sections = areaVariant?.let { areaStore.getLoadedSections(episode, it) } ?: emptyList()

        for (section in sections) {
            if (section.id in spawnSectionIds) {
                val pos = section.position
                val label = sectionIdRenderer.createPlaybackActionLabel(
                    pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat(),
                    "Spawn", SectionIdRenderer.SPAWN_COLOR,
                )
                renderContext.helpers.add(label)
                playbackLabels.add(label)
            }
        }
    }

    private fun clearPlaybackLabels() {
        for (label in playbackLabels) {
            renderContext.helpers.remove(label)
            disposeObject3DResources(label)
        }
        playbackLabels.clear()
    }

    // ---- Door helper functions and constants ----

    private fun getEffectiveDoorId(obj: QuestObjectModel): Int {
        val raw = obj.entity.data.getInt(52)
        if (raw == -1) return -1
        return if (obj.type == ObjectType.ForestDoor) raw and 0xFF else raw
    }

    private fun getSwitchAmount(obj: QuestObjectModel): Int {
        if (obj.type == ObjectType.Ruins4ButtonDoor) return 4
        if (obj.type == ObjectType.Ruins2ButtonDoor) return 2

        if (obj.type in configurableSwitchDoorTypes) {
            val amount = obj.entity.data.getInt(56)
            if (amount > 1) return amount
            return configurableSwitchDefaults[obj.type] ?: 1
        }
        return 1
    }

    private val fenceObjectTypes: Set<ObjectType> = setOf(
        ObjectType.LaserFence,
        ObjectType.LaserSquareFence,
        ObjectType.ForestLaserFenceSwitch,
        ObjectType.LaserFenceEx,
        ObjectType.LaserSquareFenceEx,
        ObjectType.RuinsLaserFence4x2,
        ObjectType.RuinsLaserFence6x2,
        ObjectType.RuinsLaserFence4x4,
        ObjectType.RuinsLaserFence6x4,
    )

    private val doorObjectTypes: Set<ObjectType> = setOf(
        ObjectType.ForestDoor,
        ObjectType.EnergyBarrier,
        ObjectType.ForestRisingBridge,
        ObjectType.Caves4ButtonDoor,
        ObjectType.CavesNormalDoor,
        ObjectType.CavesSwitchDoor,
        ObjectType.MinesDoor,
        ObjectType.MinesSwitchDoor,
        ObjectType.Ruins1Door,
        ObjectType.Ruins2Door,
        ObjectType.Ruins3Door,
        ObjectType.Ruins11ButtonDoor,
        ObjectType.Ruins21ButtonDoor,
        ObjectType.Ruins31ButtonDoor,
        ObjectType.Ruins4ButtonDoor,
        ObjectType.Ruins2ButtonDoor,
        ObjectType.LaserFence,
        ObjectType.LaserSquareFence,
        ObjectType.ForestLaserFenceSwitch,
        ObjectType.LaserFenceEx,
        ObjectType.LaserSquareFenceEx,
        ObjectType.RuinsLaserFence4x2,
        ObjectType.RuinsLaserFence6x2,
        ObjectType.RuinsLaserFence4x4,
        ObjectType.RuinsLaserFence6x4,
        ObjectType.SpaceshipDoor,
        ObjectType.TempleNormalDoor,
        ObjectType.CcaDoor,
        ObjectType.SeabedDoorWithBlueEdges,
    )

    private val configurableSwitchDoorTypes: Set<ObjectType> = setOf(
        ObjectType.Caves4ButtonDoor,
        ObjectType.MinesDoor,
        ObjectType.MinesSwitchDoor,
        ObjectType.CcaDoor,
        ObjectType.SeabedDoorWithBlueEdges,
    )

    private val configurableSwitchDefaults: Map<ObjectType, Int> = mapOf(
        ObjectType.Caves4ButtonDoor to 4,
    )
}
