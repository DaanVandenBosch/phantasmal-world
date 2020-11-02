package world.phantasmal.web.questEditor.rendering

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.lib.fileFormats.quest.Episode
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.ListValChangeEvent
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.models.AreaVariantModel
import world.phantasmal.web.questEditor.models.QuestNpcModel
import world.phantasmal.web.questEditor.models.QuestObjectModel
import world.phantasmal.web.questEditor.models.WaveModel

/**
 * Loads the necessary area and entity 3D models into [QuestRenderer].
 */
abstract class QuestMeshManager protected constructor(
    scope: CoroutineScope,
    selectedWave: Val<WaveModel?>,
    private val renderer: QuestRenderer,
    entityAssetLoader: EntityAssetLoader,
) : TrackedDisposable() {
    protected val disposer = Disposer()

    private val areaDisposer = disposer.add(Disposer())
    private val npcMeshManager = disposer.add(
        EntityMeshManager(scope, selectedWave, renderer, entityAssetLoader)
    )
    private val objectMeshManager = disposer.add(
        EntityMeshManager(scope, selectedWave, renderer, entityAssetLoader)
    )

    protected abstract fun getAreaVariantDetails(): AreaVariantDetails

    protected fun areaVariantChanged() {
        val details = getAreaVariantDetails()

        // TODO: Load area mesh.

        areaDisposer.disposeAll()
        npcMeshManager.removeAll()
        renderer.resetEntityMeshes()

        // Load entity meshes.
        areaDisposer.addAll(
            details.npcs.observeList(callNow = true, ::npcsChanged),
            details.objects.observeList(callNow = true, ::objectsChanged),
        )
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

class AreaVariantDetails(
    val episode: Episode?,
    val areaVariant: AreaVariantModel?,
    val npcs: ListVal<QuestNpcModel>,
    val objects: ListVal<QuestObjectModel>,
)
