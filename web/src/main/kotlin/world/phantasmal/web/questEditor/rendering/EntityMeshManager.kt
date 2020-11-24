package world.phantasmal.web.questEditor.rendering

import kotlinx.coroutines.*
import mu.KotlinLogging
import world.phantasmal.lib.fileFormats.quest.EntityType
import world.phantasmal.web.externals.three.Mesh
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.loading.LoadingCache
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.QuestObjectModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.DisposableContainer

private val logger = KotlinLogging.logger {}

class EntityMeshManager(
    private val scope: CoroutineScope,
    private val questEditorStore: QuestEditorStore,
    private val renderer: QuestRenderer,
    private val entityAssetLoader: EntityAssetLoader,
) : DisposableContainer() {
    /**
     * Contains one [EntityInstancedMesh] per [EntityType] and model.
     */
    private val entityMeshCache = addDisposable(
        LoadingCache<TypeAndModel, EntityInstancedMesh>(
            scope,
            { (type, model) ->
                val mesh = entityAssetLoader.loadInstancedMesh(type, model)
                renderer.entities.add(mesh)
                EntityInstancedMesh(mesh, questEditorStore.selectedWave) { entity ->
                    // When an entity's model changes, add it again. At this point it has already
                    // been removed from its previous [EntityInstancedMesh].
                    add(entity)
                }
            },
            { /* Nothing to dispose. */ },
        )
    )

    /**
     * Entity meshes that are currently being loaded.
     */
    private val loadingEntities = mutableMapOf<QuestEntityModel<*, *>, Job>()

    private var hoveredMesh: Mesh? = null
    private var selectedMesh: Mesh? = null

    init {
//        observe(questEditorStore.selectedEntity) { entity ->
//            if (entity == null) {
//                unmarkSelected()
//            } else {
//                val loaded = loadedEntities[entity]
//
//                // Mesh might not be loaded yet.
//                if (loaded == null) {
//                    unmarkSelected()
//                } else {
//                    markSelected(loaded.mesh)
//                }
//            }
//        }
    }

    override fun internalDispose() {
        removeAll()
        renderer.entities.clear()
        super.internalDispose()
    }

    fun add(entity: QuestEntityModel<*, *>) {
        loadingEntities.getOrPut(entity) {
            scope.launch {
                try {
                    val meshContainer = entityMeshCache.get(TypeAndModel(
                        type = entity.type,
                        model = (entity as? QuestObjectModel)?.model?.value
                    ))

//            if (entity == questEditorStore.selectedEntity.value) {
//                markSelected(instance)
//            }

                    meshContainer.addInstance(entity)
                    loadingEntities.remove(entity)
                } catch (e: CancellationException) {
                    // Do nothing.
                } catch (e: Throwable) {
                    loadingEntities.remove(entity)
                    logger.error(e) {
                        "Couldn't load mesh for entity of type ${entity.type}."
                    }
                }
            }
        }
    }

    fun remove(entity: QuestEntityModel<*, *>) {
        loadingEntities.remove(entity)?.cancel()

        entityMeshCache.getIfPresentNow(
            TypeAndModel(
                entity.type,
                (entity as? QuestObjectModel)?.model?.value
            )
        )?.removeInstance(entity)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun removeAll() {
        loadingEntities.values.forEach { it.cancel() }
        loadingEntities.clear()

        for (meshContainerDeferred in entityMeshCache.values) {
            if (meshContainerDeferred.isCompleted) {
                meshContainerDeferred.getCompleted().clearInstances()
            }
        }
    }

//    private fun markSelected(entityMesh: AbstractMesh) {
//        if (entityMesh == hoveredMesh) {
//            hoveredMesh = null
//        }
//
//        if (entityMesh != selectedMesh) {
//            selectedMesh?.let { it.showBoundingBox = false }
//
//            entityMesh.showBoundingBox = true
//        }
//
//        selectedMesh = entityMesh
//    }
//
//    private fun unmarkSelected() {
//        selectedMesh?.let { it.showBoundingBox = false }
//        selectedMesh = null
//    }

    private data class TypeAndModel(val type: EntityType, val model: Int?)
}
