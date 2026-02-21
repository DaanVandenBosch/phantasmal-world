package world.phantasmal.web.questEditor.rendering

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.list.ListChangeEvent
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.DisposableSupervisedScope
import world.phantasmal.psolib.Episode
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.models.AreaVariantModel
import world.phantasmal.web.questEditor.models.QuestNpcModel
import world.phantasmal.web.questEditor.models.QuestObjectModel
import world.phantasmal.web.questEditor.stores.AreaStore
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.web.externals.three.Group

/**
 * Loads the necessary area and entity 3D models into [QuestRenderer].
 */
abstract class QuestMeshManager protected constructor(
    areaAssetLoader: AreaAssetLoader,
    entityAssetLoader: EntityAssetLoader,
    private val questEditorStore: QuestEditorStore,
    areaStore: AreaStore,
    private val renderContext: QuestRenderContext,
) : DisposableContainer() {
    private val scope = addDisposable(DisposableSupervisedScope(this::class, Dispatchers.Default))
    private val areaMeshManager = AreaMeshManager(renderContext, areaAssetLoader)
    private val npcMeshManager = addDisposable(
        EntityMeshManager(
            questEditorStore,
            renderContext,
            entityAssetLoader,
            areaStore,
            enableSectionLabels = true
        ) // Only NPC manager handles section labels
    )
    private val objectMeshManager = addDisposable(
        EntityMeshManager(
            questEditorStore,
            renderContext,
            entityAssetLoader,
            areaStore,
            enableSectionLabels = false
        ) // Object manager doesn't handle section labels
    )

    // Origin point rendering
    private val originPointRenderer = OriginPointRenderer()
    private var originGroup: Group? = null

    private var areaLoadJob: Job? = null
    private var npcLoadJob: Job? = null
    private var objectLoadJob: Job? = null

    private var npcObserver: Disposable? = null
    private var objectObserver: Disposable? = null

    init {
        // Observe origin point show/hide state
        observeNow(questEditorStore.showOriginPoint) { showOrigin ->
            updateOriginPointVisibility(showOrigin)
        }
    }

    protected fun loadAreaMeshes(episode: Episode?, areaVariant: AreaVariantModel?) {
        areaLoadJob?.cancel()
        areaLoadJob = scope.launch {
            areaMeshManager.load(episode, areaVariant)
        }
    }

    protected fun loadNpcMeshes(npcs: ListCell<QuestNpcModel>) {
        npcLoadJob?.cancel()
        npcLoadJob = scope.launch {
            npcObserver?.dispose()
            npcMeshManager.removeAll()

            npcs.value.forEach(npcMeshManager::add)

            npcObserver = npcs.observeListChange(::npcsChanged)
        }
    }

    protected fun loadObjectMeshes(objects: ListCell<QuestObjectModel>) {
        objectLoadJob?.cancel()
        objectLoadJob = scope.launch {
            objectObserver?.dispose()
            objectMeshManager.removeAll()

            objects.value.forEach(objectMeshManager::add)

            objectObserver = objects.observeListChange(::objectsChanged)
        }
    }

    private fun npcsChanged(event: ListChangeEvent<QuestNpcModel>) {
        for (change in event.changes) {
            change.removed.forEach(npcMeshManager::remove)
            change.inserted.forEach(npcMeshManager::add)
        }
    }

    private fun objectsChanged(event: ListChangeEvent<QuestObjectModel>) {
        for (change in event.changes) {
            change.removed.forEach(objectMeshManager::remove)
            change.inserted.forEach(objectMeshManager::add)
        }
    }

    /**
     * Updates the visibility of the origin point based on the store state.
     */
    private fun updateOriginPointVisibility(showOrigin: Boolean) {
        if (showOrigin) {
            if (originGroup == null) {
                originGroup = originPointRenderer.createOriginPointVisualization()
                renderContext.scene.add(originGroup!!)
            }
        } else {
            originGroup?.let { group ->
                renderContext.scene.remove(group)
                originGroup = null
            }
        }
    }

    /**
     * Called before each render to update text scales for constant screen size.
     */
    fun beforeRender() {
        // Update text scales in the NPC mesh manager (which handles section labels)
        npcMeshManager.beforeRender()

        // No text scaling needed for origin point since text was removed
    }
}
