package world.phantasmal.web.questEditor.rendering

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.lib.fileFormats.quest.Episode
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.ListValChangeEvent
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.models.AreaVariantModel
import world.phantasmal.web.questEditor.models.QuestNpcModel
import world.phantasmal.web.questEditor.models.QuestObjectModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore

/**
 * Loads the necessary area and entity 3D models into [QuestRenderer].
 */
abstract class QuestMeshManager protected constructor(
    private val scope: CoroutineScope,
    questEditorStore: QuestEditorStore,
    private val renderer: QuestRenderer,
    areaAssetLoader: AreaAssetLoader,
    entityAssetLoader: EntityAssetLoader,
) : TrackedDisposable() {
    protected val disposer = Disposer()

    private val areaDisposer = disposer.add(Disposer())
    private val areaMeshManager = AreaMeshManager(renderer, areaAssetLoader)
    private val npcMeshManager = disposer.add(
        EntityMeshManager(scope, questEditorStore, renderer, entityAssetLoader)
    )
    private val objectMeshManager = disposer.add(
        EntityMeshManager(scope, questEditorStore, renderer, entityAssetLoader)
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
            npcMeshManager.removeAll()
            objectMeshManager.removeAll()

            renderer.resetCamera()

            // Load area model.
            areaMeshManager.load(episode, areaVariant)

            // Load entity meshes.
            areaDisposer.addAll(
                npcs.observeList(callNow = true, ::npcsChanged),
                objects.observeList(callNow = true, ::objectsChanged),
            )
        }
    }

    override fun internalDispose() {
        disposer.dispose()
        super.internalDispose()
    }

    private fun npcsChanged(change: ListValChangeEvent<QuestNpcModel>) {
        if (change is ListValChangeEvent.Change) {
            npcMeshManager.remove(change.removed)
            npcMeshManager.add(change.inserted)
        }
    }

    private fun objectsChanged(change: ListValChangeEvent<QuestObjectModel>) {
        if (change is ListValChangeEvent.Change) {
            objectMeshManager.remove(change.removed)
            objectMeshManager.add(change.inserted)
        }
    }
}
