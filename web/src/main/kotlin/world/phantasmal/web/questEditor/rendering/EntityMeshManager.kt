package world.phantasmal.web.questEditor.rendering

import kotlinx.coroutines.*
import mu.KotlinLogging
import world.phantasmal.lib.fileFormats.quest.EntityType
import world.phantasmal.web.externals.three.BoxHelper
import world.phantasmal.web.externals.three.Color
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
                    // been removed from its previous EntityInstancedMesh.
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

    private var highlightedEntityInstance: EntityInstance? = null
    private var selectedEntityInstance: EntityInstance? = null

    /**
     * Bounding box around the highlighted entity.
     */
    private val highlightedBox = BoxHelper(color = Color(0.7, 0.7, 0.7)).apply {
        visible = false
        renderer.scene.add(this)
    }

    /**
     * Bounding box around the selected entity.
     */
    private val selectedBox = BoxHelper(color = Color(0.9, 0.9, 0.9)).apply {
        visible = false
        renderer.scene.add(this)
    }

    init {
        observe(questEditorStore.highlightedEntity) { entity ->
            if (entity == null) {
                unmarkHighlighted()
            } else {
                val instance = getEntityInstance(entity)

                // Mesh might not be loaded yet.
                if (instance == null) {
                    unmarkHighlighted()
                } else {
                    markHighlighted(instance)
                }
            }
        }

        observe(questEditorStore.selectedEntity) { entity ->
            if (entity == null) {
                unmarkSelected()
            } else {
                val instance = getEntityInstance(entity)

                // Mesh might not be loaded yet.
                if (instance == null) {
                    unmarkSelected()
                } else {
                    markSelected(instance)
                }
            }
        }
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

                    val instance = meshContainer.addInstance(entity)
                    loadingEntities.remove(entity)

                    if (entity == questEditorStore.selectedEntity.value) {
                        markSelected(instance)
                    } else if (entity == questEditorStore.highlightedEntity.value) {
                        markHighlighted(instance)
                    }
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

    private fun markHighlighted(instance: EntityInstance) {
        if (instance == selectedEntityInstance) {
            highlightedEntityInstance?.follower = null
            highlightedEntityInstance = null
            highlightedBox.visible = false
            return
        }

        if (instance != highlightedEntityInstance) {
            highlightedEntityInstance?.follower = null

            highlightedBox.setFromObject(instance.mesh)
            instance.follower = highlightedBox
            highlightedBox.visible = true
        }

        highlightedEntityInstance = instance
    }

    private fun unmarkHighlighted() {
        highlightedEntityInstance?.let { highlighted ->
            if (highlighted != selectedEntityInstance) {
                highlighted.follower = null
            }

            highlightedEntityInstance = null
            highlightedBox.visible = false
        }
    }

    private fun markSelected(instance: EntityInstance) {
        if (instance == highlightedEntityInstance) {
            highlightedBox.visible = false
        }

        if (instance != selectedEntityInstance) {
            selectedEntityInstance?.follower = null

            selectedBox.setFromObject(instance.mesh)
            instance.follower = selectedBox
            selectedBox.visible = true
        }

        selectedEntityInstance = instance
    }

    private fun unmarkSelected() {
        selectedEntityInstance?.let { selected ->
            if (selected == highlightedEntityInstance) {
                highlightedBox.setFromObject(selected.mesh)
                selected.follower = highlightedBox
                highlightedBox.visible = true
            } else {
                selected.follower = null
            }

            selectedEntityInstance = null
            selectedBox.visible = false
        }
    }

    private fun getEntityInstance(entity: QuestEntityModel<*, *>): EntityInstance? =
        entityMeshCache.getIfPresentNow(
            TypeAndModel(
                entity.type,
                (entity as? QuestObjectModel)?.model?.value
            )
        )?.getInstance(entity)

    private data class TypeAndModel(val type: EntityType, val model: Int?)
}
