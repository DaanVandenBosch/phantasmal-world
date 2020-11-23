package world.phantasmal.web.questEditor.rendering

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import world.phantasmal.lib.fileFormats.quest.EntityType
import world.phantasmal.observable.value.Val
import world.phantasmal.web.externals.three.Group
import world.phantasmal.web.externals.three.InstancedMesh
import world.phantasmal.web.externals.three.Mesh
import world.phantasmal.web.externals.three.Object3D
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.loading.LoadingCache
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.QuestNpcModel
import world.phantasmal.web.questEditor.models.QuestObjectModel
import world.phantasmal.web.questEditor.models.WaveModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.DisposableContainer

private val logger = KotlinLogging.logger {}

class EntityMeshManager(
    private val scope: CoroutineScope,
    private val questEditorStore: QuestEditorStore,
    private val renderer: QuestRenderer,
    private val entityAssetLoader: EntityAssetLoader,
) : DisposableContainer() {
    private val entityMeshes = Group().apply { name = "Entities" }

    private val meshCache = addDisposable(
        LoadingCache<CacheKey, InstancedMesh>(
            scope,
            { (type, model) ->
                val mesh = entityAssetLoader.loadInstancedMesh(type, model)
                entityMeshes.add(mesh)
                mesh
            },
            { /* Nothing to dispose. */ },
        )
    )

    private val queue: MutableList<QuestEntityModel<*, *>> = mutableListOf()
    private val loadedEntities: MutableList<LoadedEntity> = mutableListOf()
    private var loading = false

    private var hoveredMesh: Mesh? = null
    private var selectedMesh: Mesh? = null

    init {
        renderer.scene.add(entityMeshes)

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
        renderer.scene.remove(entityMeshes)
        removeAll()
        entityMeshes.clear()
        super.internalDispose()
    }

    fun add(entity: QuestEntityModel<*, *>) {
        queue.add(entity)

        if (!loading) {
            loading = true

            scope.launch {
                try {
                    while (queue.isNotEmpty()) {
                        val queuedEntity = queue.first()

                        try {
                            load(queuedEntity)
                        } catch (e: Error) {
                            logger.error(e) {
                                "Couldn't load model for entity of type ${queuedEntity.type}."
                            }
                            queue.remove(queuedEntity)
                        }
                    }
                } finally {
                    loading = false
                }
            }
        }
    }

    fun remove(entity: QuestEntityModel<*, *>) {
        queue.remove(entity)

        val idx = loadedEntities.indexOfFirst { it.entity == entity }

        if (idx != -1) {
            val loaded = loadedEntities.removeAt(idx)
            loaded.mesh.count--

            for (i in idx until loaded.mesh.count) {
                loaded.mesh.instanceMatrix.copyAt(i, loaded.mesh.instanceMatrix, i + 1)
                loadedEntities[i].instanceIndex = i
            }

            loaded.dispose()
        }
    }

    fun removeAll() {
        for (loaded in loadedEntities) {
            loaded.mesh.count = 0
            loaded.dispose()
        }

        loadedEntities.clear()
        queue.clear()
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

    private suspend fun load(entity: QuestEntityModel<*, *>) {
        val mesh = meshCache.get(CacheKey(
            type = entity.type,
            model = (entity as? QuestObjectModel)?.model?.value
        ))

        // Only add an instance of this mesh if the entity is still in the queue at this point.
        if (queue.remove(entity)) {
            val instanceIndex = mesh.count
            mesh.count++

//            if (entity == questEditorStore.selectedEntity.value) {
//                markSelected(instance)
//            }

            loadedEntities.add(LoadedEntity(
                entity,
                mesh,
                instanceIndex,
                questEditorStore.selectedWave
            ))
        }
    }

    private data class CacheKey(val type: EntityType, val model: Int?)

    private inner class LoadedEntity(
        val entity: QuestEntityModel<*, *>,
        val mesh: InstancedMesh,
        var instanceIndex: Int,
        selectedWave: Val<WaveModel?>,
    ) : DisposableContainer() {
        init {
            updateMatrix()

            addDisposables(
                entity.worldPosition.observe { updateMatrix() },
                entity.worldRotation.observe { updateMatrix() },
            )

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
                isVisible = entity.section.isNotNull()

                if (entity is QuestObjectModel) {
                    addDisposable(entity.model.observe(callNow = false) {
                        remove(entity)
                        add(entity)
                    })
                }
            }

//            observe(isVisible) { visible ->
//                mesh.setEnabled(visible)
//            }
        }

        override fun internalDispose() {
            // TODO: Dispose instance.
            super.internalDispose()
        }

        private fun updateMatrix() {
            instanceHelper.position.set(
                entity.worldPosition.value.x,
                entity.worldPosition.value.y,
                entity.worldPosition.value.z,
            )
            instanceHelper.rotation.set(
                entity.worldRotation.value.x,
                entity.worldRotation.value.y,
                entity.worldRotation.value.z,
            )
            instanceHelper.updateMatrix()
            mesh.setMatrixAt(instanceIndex, instanceHelper.matrix)
            mesh.instanceMatrix.needsUpdate = true
        }
    }

    companion object {
        private val instanceHelper = Object3D()
    }
}
