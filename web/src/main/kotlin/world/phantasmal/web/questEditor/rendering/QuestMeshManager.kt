package world.phantasmal.web.questEditor.rendering

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.DisposableSupervisedScope
import world.phantasmal.lib.Episode
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.ListValChangeEvent
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.models.AreaVariantModel
import world.phantasmal.web.questEditor.models.QuestNpcModel
import world.phantasmal.web.questEditor.models.QuestObjectModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.DisposableContainer

/**
 * Loads the necessary area and entity 3D models into [QuestRenderer].
 */
abstract class QuestMeshManager protected constructor(
    areaAssetLoader: AreaAssetLoader,
    entityAssetLoader: EntityAssetLoader,
    questEditorStore: QuestEditorStore,
    renderContext: QuestRenderContext,
) : DisposableContainer() {
    private val scope = addDisposable(DisposableSupervisedScope(this::class, Dispatchers.Default))
    private val areaMeshManager = AreaMeshManager(renderContext, areaAssetLoader)
    private val npcMeshManager = addDisposable(
        EntityMeshManager(questEditorStore, renderContext, entityAssetLoader)
    )
    private val objectMeshManager = addDisposable(
        EntityMeshManager(questEditorStore, renderContext, entityAssetLoader)
    )

    private var areaLoadJob: Job? = null
    private var npcLoadJob: Job? = null
    private var objectLoadJob: Job? = null

    private var npcObserver: Disposable? = null
    private var objectObserver: Disposable? = null

    protected fun loadAreaMeshes(episode: Episode?, areaVariant: AreaVariantModel?) {
        areaLoadJob?.cancel()
        areaLoadJob = scope.launch {
            areaMeshManager.load(episode, areaVariant)
        }
    }

    protected fun loadNpcMeshes(npcs: ListVal<QuestNpcModel>) {
        npcLoadJob?.cancel()
        npcLoadJob = scope.launch {
            npcObserver?.dispose()
            npcMeshManager.removeAll()

            npcObserver = npcs.observeList(callNow = true, ::npcsChanged)
        }
    }

    protected fun loadObjectMeshes(objects: ListVal<QuestObjectModel>) {
        objectLoadJob?.cancel()
        objectLoadJob = scope.launch {
            objectObserver?.dispose()
            objectMeshManager.removeAll()

            objectObserver = objects.observeList(callNow = true, ::objectsChanged)
        }
    }

    private fun npcsChanged(change: ListValChangeEvent<QuestNpcModel>) {
        if (change is ListValChangeEvent.Change) {
            change.removed.forEach(npcMeshManager::remove)
            change.inserted.forEach(npcMeshManager::add)
        }
    }

    private fun objectsChanged(change: ListValChangeEvent<QuestObjectModel>) {
        if (change is ListValChangeEvent.Change) {
            change.removed.forEach(objectMeshManager::remove)
            change.inserted.forEach(objectMeshManager::add)
        }
    }
}
