package world.phantasmal.web.questEditor.rendering

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.observable.value.Val
import world.phantasmal.web.externals.babylon.AbstractMesh
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.QuestNpcModel
import world.phantasmal.web.questEditor.models.WaveModel
import world.phantasmal.web.questEditor.rendering.conversion.EntityMetadata

private val logger = KotlinLogging.logger {}

private class LoadedEntity(val entity: QuestEntityModel<*, *>, val disposer: Disposer)

class EntityMeshManager(
    private val scope: CoroutineScope,
    private val selectedWave: Val<WaveModel?>,
    private val renderer: QuestRenderer,
    private val entityAssetLoader: EntityAssetLoader,
) : TrackedDisposable() {
    private val queue: MutableList<QuestEntityModel<*, *>> = mutableListOf()
    private val loadedEntities: MutableList<LoadedEntity> = mutableListOf()
    private var loading = false

    override fun internalDispose() {
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

            val loadedIndex = loadedEntities.indexOfFirst { it.entity == entity }

            if (loadedIndex != -1) {
                val loaded = loadedEntities.removeAt(loadedIndex)

                renderer.removeEntityMesh(loaded.entity)
                loaded.disposer.dispose()
            }
        }
    }

    fun removeAll() {
        for (loaded in loadedEntities) {
            loaded.disposer.dispose()
        }

        loadedEntities.clear()
        queue.clear()
    }

    private suspend fun load(entity: QuestEntityModel<*, *>) {
        // TODO
        val mesh = entityAssetLoader.loadMesh(entity.type, model = null)

        // Only add an instance of this mesh if the entity is still in the queue at this point.
        if (queue.remove(entity)) {
            val instance = mesh.createInstance(entity.type.uniqueName)
            instance.metadata = EntityMetadata(entity)
            instance.position = entity.worldPosition.value
            updateEntityMesh(entity, instance)
        }
    }

    private fun updateEntityMesh(entity: QuestEntityModel<*, *>, mesh: AbstractMesh) {
        renderer.addEntityMesh(mesh)

        val disposer = Disposer(
            entity.worldPosition.observe { (pos) ->
                mesh.position = pos
                renderer.scheduleRender()
            },

            // TODO: Rotation.
//            entity.worldRotation.observe { (value) ->
//                mesh.rotation.copy(value)
//                renderer.schedule_render()
//            },

            // TODO: Model.
//            entity.model.observe {
//                remove(listOf(entity))
//                add(listOf(entity))
//            },
        )

        if (entity is QuestNpcModel) {
            disposer.add(
                selectedWave
                    .map(entity.wave) { selectedWave, entityWave ->
                        selectedWave == null || selectedWave == entityWave
                    }
                    .observe(callNow = true) { (visible) ->
                        mesh.setEnabled(visible)
                        renderer.scheduleRender()
                    },
            )
        }

        loadedEntities.add(LoadedEntity(entity, disposer))
    }
}
