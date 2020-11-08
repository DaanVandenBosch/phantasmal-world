package world.phantasmal.web.questEditor.rendering

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import world.phantasmal.observable.value.Val
import world.phantasmal.web.externals.babylon.AbstractMesh
import world.phantasmal.web.externals.babylon.TransformNode
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.QuestNpcModel
import world.phantasmal.web.questEditor.models.QuestObjectModel
import world.phantasmal.web.questEditor.models.WaveModel
import world.phantasmal.web.questEditor.rendering.conversion.EntityMetadata
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.DisposableContainer

private val logger = KotlinLogging.logger {}

class EntityMeshManager(
    private val scope: CoroutineScope,
    private val questEditorStore: QuestEditorStore,
    renderer: QuestRenderer,
    private val entityAssetLoader: EntityAssetLoader,
) : DisposableContainer() {
    private val queue: MutableList<QuestEntityModel<*, *>> = mutableListOf()
    private val loadedEntities: MutableMap<QuestEntityModel<*, *>, LoadedEntity> = mutableMapOf()
    private var loading = false

    private var entityMeshes = TransformNode("Entities", renderer.scene)
    private var hoveredMesh: AbstractMesh? = null
    private var selectedMesh: AbstractMesh? = null

    init {
        observe(questEditorStore.selectedEntity) { entity ->
            if (entity == null) {
                unmarkSelected()
            } else {
                val loaded = loadedEntities[entity]

                // Mesh might not be loaded yet.
                if (loaded == null) {
                    unmarkSelected()
                } else {
                    markSelected(loaded.mesh)
                }
            }
        }
    }

    override fun internalDispose() {
        entityMeshes.dispose()
        removeAll()
        super.internalDispose()
    }

    fun add(entities: List<QuestEntityModel<*, *>>) {
        queue.addAll(entities)

        if (!loading) {
            scope.launch {
                try {
                    loading = true

                    while (queue.isNotEmpty()) {
                        val entity = queue.first()

                        try {
                            load(entity)
                        } catch (e: Error) {
                            logger.error(e) {
                                "Couldn't load model for entity of type ${entity.type}."
                            }
                            queue.remove(entity)
                        }
                    }
                } finally {
                    loading = false
                }
            }
        }
    }

    fun remove(entities: List<QuestEntityModel<*, *>>) {
        for (entity in entities) {
            queue.remove(entity)

            loadedEntities.remove(entity)?.dispose()
        }
    }

    fun removeAll() {
        for (loaded in loadedEntities.values) {
            loaded.dispose()
        }

        loadedEntities.clear()
        queue.clear()
    }

    private fun markSelected(entityMesh: AbstractMesh) {
        if (entityMesh == hoveredMesh) {
            hoveredMesh = null
        }

        if (entityMesh != selectedMesh) {
            selectedMesh?.let { it.showBoundingBox = false }

            entityMesh.showBoundingBox = true
        }

        selectedMesh = entityMesh
    }

    private fun unmarkSelected() {
        selectedMesh?.let { it.showBoundingBox = false }
        selectedMesh = null
    }

    private suspend fun load(entity: QuestEntityModel<*, *>) {
        val mesh = entityAssetLoader.loadMesh(
            type = entity.type,
            model = (entity as? QuestObjectModel)?.model?.value
        )

        // Only add an instance of this mesh if the entity is still in the queue at this point.
        if (queue.remove(entity)) {
            val instance = mesh.createInstance(entity.type.uniqueName)
            instance.parent = entityMeshes

            if (entity == questEditorStore.selectedEntity.value) {
                markSelected(instance)
            }

            loadedEntities[entity] = LoadedEntity(entity, instance, questEditorStore.selectedWave)
        }
    }

    private inner class LoadedEntity(
        entity: QuestEntityModel<*, *>,
        val mesh: AbstractMesh,
        selectedWave: Val<WaveModel?>,
    ) : DisposableContainer() {
        init {
            mesh.metadata = EntityMetadata(entity)

            observe(entity.worldPosition) { pos ->
                mesh.position = pos
            }

            observe(entity.worldRotation) { rot ->
                mesh.rotation = rot
            }

            val isVisible: Val<Boolean>

            if (entity is QuestNpcModel) {
                isVisible =
                    entity.sectionInitialized.map(
                        selectedWave,
                        entity.wave
                    ) { sectionInitialized, sWave, entityWave ->
                        sectionInitialized && (sWave == null || sWave == entityWave)
                    }
            } else {
                isVisible = entity.section.map { section -> section != null }

                if (entity is QuestObjectModel) {
                    addDisposable(entity.model.observe(callNow = false) {
                        remove(listOf(entity))
                        add(listOf(entity))
                    })
                }
            }

            observe(isVisible) { visible ->
                mesh.setEnabled(visible)
            }
        }

        override fun internalDispose() {
            mesh.parent = null
            mesh.dispose()
            super.internalDispose()
        }
    }
}
