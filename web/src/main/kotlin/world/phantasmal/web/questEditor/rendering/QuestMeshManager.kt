package world.phantasmal.web.questEditor.rendering

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.lib.fileFormats.quest.Episode
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
    private val scope: CoroutineScope,
    areaAssetLoader: AreaAssetLoader,
    entityAssetLoader: EntityAssetLoader,
    questEditorStore: QuestEditorStore,
    renderContext: QuestRenderContext,
) : DisposableContainer() {
    private val areaDisposer = addDisposable(Disposer())
    private val areaMeshManager = AreaMeshManager(renderContext, areaAssetLoader)
    private val entityMeshManager = addDisposable(
        EntityMeshManager(scope, questEditorStore, renderContext, entityAssetLoader)
    )

    private var loadJob: Job? = null

    protected fun loadMeshes(
        episode: Episode?,
        areaVariant: AreaVariantModel?,
        npcs: ListVal<QuestNpcModel>,
        objects: ListVal<QuestObjectModel>,
    ) {
        loadJob?.cancel()
        loadJob = scope.launch {
            // Reset models.
            areaDisposer.disposeAll()
            entityMeshManager.removeAll()

            // Load area model.
            areaMeshManager.load(episode, areaVariant)

            // Load entity meshes.
            areaDisposer.addAll(
                npcs.observeList(callNow = true, ::npcsChanged),
                objects.observeList(callNow = true, ::objectsChanged),
            )
        }
    }

    private fun npcsChanged(change: ListValChangeEvent<QuestNpcModel>) {
        if (change is ListValChangeEvent.Change) {
            change.removed.forEach(entityMeshManager::remove)
            change.inserted.forEach(entityMeshManager::add)
        }
    }

    private fun objectsChanged(change: ListValChangeEvent<QuestObjectModel>) {
        if (change is ListValChangeEvent.Change) {
            change.removed.forEach(entityMeshManager::remove)
            change.inserted.forEach(entityMeshManager::add)
        }
    }
}
