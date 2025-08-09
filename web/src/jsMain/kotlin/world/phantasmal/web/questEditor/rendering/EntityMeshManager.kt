package world.phantasmal.web.questEditor.rendering

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.khronos.webgl.Float32Array
import world.phantasmal.core.disposable.DisposableSupervisedScope
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.cell.observeNow
import world.phantasmal.psolib.fileFormats.quest.EntityType
import world.phantasmal.psolib.fileFormats.quest.ObjectType
import world.phantasmal.web.core.rendering.disposeObject3DResources
import world.phantasmal.web.externals.three.*
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.core.loading.LoadingCache
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.QuestObjectModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.obj

private val logger = KotlinLogging.logger {}

class EntityMeshManager(
    private val questEditorStore: QuestEditorStore,
    private val renderContext: QuestRenderContext,
    private val entityAssetLoader: EntityAssetLoader,
) : DisposableContainer() {
    private val scope = addDisposable(DisposableSupervisedScope(this::class, Dispatchers.Main))
    private val rangeCircleRenderer = RangeCircleRenderer()

    /**
     * Contains one [EntityInstanceContainer] per [EntityType] and model.
     */
    private val entityMeshCache = addDisposable(
        LoadingCache<TypeAndModel, EntityInstanceContainer>(
            { (type, model) ->
                val mesh = entityAssetLoader.loadInstancedMesh(type, model)
                renderContext.entities.add(mesh)
                EntityInstanceContainer(mesh, modelChanged = { entity ->
                    // When an entity's model changes, add it again. At this point it has already
                    // been removed from its previous EntityInstancedMesh.
                    add(entity)
                })
            },
            EntityInstanceContainer::dispose,
        )
    )

    /**
     * Warp destinations.
     */
    private val destinationInstanceContainer = addDisposable(
        DestinationInstanceContainer().also {
            renderContext.entities.add(it.mesh)
        }
    )

    // Lines between warps and their destination.
    private val warpLineBufferAttribute = Float32BufferAttribute(Float32Array(6), 3)
    private val warpLines =
        LineSegments(
            BufferGeometry().setAttribute("position", warpLineBufferAttribute),
            LineBasicMaterial(obj {
                color = DestinationInstanceContainer.COLOR
            })
        ).also {
            it.visible = false
            it.frustumCulled = false
            renderContext.helpers.add(it)
        }
    private var warpLineDisposer = addDisposable(Disposer())

    /**
     * Entity meshes that are currently being loaded.
     */
    private val loadingEntities = mutableMapOf<QuestEntityModel<*, *>, Job>()

    /**
     * Range circle for the currently selected entity (if it has radius properties).
     */
    private var selectedEntityRangeCircle: Object3D? = null
    
    /**
     * Disposer for selected entity range circle observers.
     */
    private var rangeCircleObserverDisposer = addDisposable(Disposer())

    private var highlightedEntityInstance: EntityInstance? = null
    private var selectedEntityInstance: EntityInstance? = null

    /**
     * Bounding box around the highlighted entity.
     */
    private val highlightedBox = BoxHelper(color = Color(.7, .7, .7)).apply {
        visible = false
        renderContext.scene.add(this)
    }

    /**
     * Bounding box around the selected entity.
     */
    private val selectedBox = BoxHelper(color = Color(.9, .9, .9)).apply {
        visible = false
        renderContext.scene.add(this)
    }

    init {
        observeNow(questEditorStore.highlightedEntity) { entity ->
            // getEntityInstance can return null at this point because the entity mesh might not be
            // loaded yet.
            markHighlighted(entity?.let(::getEntityInstance))
        }

        observeNow(questEditorStore.selectedEntity) { entity ->
            // getEntityInstance can return null at this point because the entity mesh might not be
            // loaded yet.
            markSelected(entity?.let(::getEntityInstance))
            
            // Update range circle for selected entity
            updateSelectedEntityRangeCircle(entity)
        }
    }

    override fun dispose() {
        removeAll()
        renderContext.entities.clear()
        disposeObject3DResources(warpLines)
        // Dispose selected entity range circle
        clearSelectedEntityRangeCircle()
        super.dispose()
    }

    fun add(entity: QuestEntityModel<*, *>) {
        loadingEntities.getOrPut(entity) {
            scope.launch {
                try {
                    val entityInstancedMesh = entityMeshCache.get(
                        TypeAndModel(
                            type = entity.type,
                            model = (entity as? QuestObjectModel)?.model?.value
                        )
                    )

                    val instance = entityInstancedMesh.addInstance(entity)

                    if (entity == questEditorStore.selectedEntity.value) {
                        markSelected(instance)
                    } else if (entity == questEditorStore.highlightedEntity.value) {
                        markHighlighted(instance)
                    }

                    if (entity is QuestObjectModel && entity.hasDestination) {
                        destinationInstanceContainer.addInstance(entity)
                    }
                } catch (e: CancellationException) {
                    logger.trace(e) {
                        "Mesh loading for entity of type ${entity.type} cancelled."
                    }
                } catch (e: Throwable) {
                    logger.error(e) {
                        "Couldn't load mesh for entity of type ${entity.type}."
                    }
                } finally {
                    loadingEntities.remove(entity)
                }
            }
        }
    }

    fun remove(entity: QuestEntityModel<*, *>) {
        loadingEntities.remove(entity)?.cancel("Removed.")

        entityMeshCache.getIfPresentNow(
            TypeAndModel(
                entity.type,
                (entity as? QuestObjectModel)?.model?.value
            )
        )?.removeInstance(entity)

        if (entity is QuestObjectModel && entity.hasDestination) {
            destinationInstanceContainer.removeInstance(entity)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun removeAll() {
        loadingEntities.values.forEach { it.cancel("Removed.") }
        loadingEntities.clear()

        for (meshContainerDeferred in entityMeshCache.values) {
            if (meshContainerDeferred.isCompleted) {
                meshContainerDeferred.getCompleted().clearInstances()
            }
        }

        destinationInstanceContainer.clearInstances()
    }

    private fun markHighlighted(instance: EntityInstance?) {
        if (instance == selectedEntityInstance) {
            highlightedEntityInstance?.follower = null
            highlightedEntityInstance = null
            highlightedBox.visible = false
        } else {
            attachBoxHelper(
                highlightedBox,
                highlightedEntityInstance,
                instance,
            )
            highlightedEntityInstance = instance
        }
    }

    private fun markSelected(instance: EntityInstance?) {
        if (instance == highlightedEntityInstance) {
            highlightedBox.visible = false
            highlightedEntityInstance = null
        }

        attachBoxHelper(selectedBox, selectedEntityInstance, instance)
        attachWarpLine(instance)
        selectedEntityInstance = instance
    }

    private fun attachBoxHelper(
        box: BoxHelper,
        oldInstance: EntityInstance?,
        newInstance: EntityInstance?,
    ) {
        box.visible = newInstance != null

        if (oldInstance == newInstance) return

        oldInstance?.follower = null

        if (newInstance != null) {
            box.setFromObject(newInstance.mesh)
            newInstance.follower = box
            box.visible = true
        }
    }

    private fun attachWarpLine(newInstance: EntityInstance?) {
        warpLineDisposer.disposeAll()
        warpLines.visible = false

        if (newInstance != null &&
            newInstance.entity is QuestObjectModel &&
            newInstance.entity.hasDestination
        ) {
            warpLineDisposer.add(newInstance.entity.worldPosition.observeNow {
                warpLineBufferAttribute.setXYZ(0, it.x, it.y, it.z)
                warpLineBufferAttribute.needsUpdate = true
            })
            warpLineDisposer.add(newInstance.entity.destinationPosition.observeNow {
                warpLineBufferAttribute.setXYZ(1, it.x, it.y, it.z)
                warpLineBufferAttribute.needsUpdate = true
            })
            warpLines.visible = true
        }
    }

    private fun getEntityInstance(entity: QuestEntityModel<*, *>): EntityInstance? =
        entityMeshCache.getIfPresentNow(
            TypeAndModel(
                entity.type,
                (entity as? QuestObjectModel)?.model?.value
            )
        )?.getInstance(entity)

    /**
     * Updates the range circle for the currently selected entity.
     * Shows range circle only for selected EventCollision objects.
     */
    private fun updateSelectedEntityRangeCircle(entity: QuestEntityModel<*, *>?) {
        // Clear existing range circle and observers
        clearSelectedEntityRangeCircle()
        
        // Add range circle for selected EventCollision object
        if (entity is QuestObjectModel && entity.type == ObjectType.EventCollision) {
            createRangeCircleForEntity(entity)
        }
    }
    
    /**
     * Clears the current selected entity range circle and its observers.
     */
    private fun clearSelectedEntityRangeCircle() {
        selectedEntityRangeCircle?.let { circle ->
            renderContext.helpers.remove(circle)
            disposeObject3DResources(circle)
        }
        selectedEntityRangeCircle = null
        rangeCircleObserverDisposer.disposeAll()
    }
    
    /**
     * Creates and displays a range circle for the specified EventCollision entity.
     */
    private fun createRangeCircleForEntity(entity: QuestObjectModel) {
        // Find the radius property
        val radiusProp = entity.properties.value.find { it.name == "Radius" }
        if (radiusProp != null) {
            val radius = radiusProp.value.value as? Float ?: return
            if (radius > 0) {
                createAndDisplayRangeCircle(entity, radius)
                
                // Observe property changes to recreate the circle when radius changes
                rangeCircleObserverDisposer.add(radiusProp.value.observeNow { newRadius ->
                    val newRadiusFloat = newRadius as? Float ?: return@observeNow
                    if (questEditorStore.selectedEntity.value == entity) {
                        if (newRadiusFloat > 0) {
                            // Recreate circle with new radius (only if this entity is still selected)
                            recreateRangeCircle(entity, newRadiusFloat)
                        } else {
                            // Hide circle if radius is 0 or negative
                            clearSelectedEntityRangeCircle()
                        }
                    }
                })
                
                // Observe position changes to update the circle position
                rangeCircleObserverDisposer.add(entity.worldPosition.observeNow { newPosition ->
                    if (questEditorStore.selectedEntity.value == entity && selectedEntityRangeCircle != null) {
                        // Update position (only if this entity is still selected)
                        val currentRadius = radiusProp.value.value as? Float ?: return@observeNow
                        if (currentRadius > 0) {
                            rangeCircleRenderer.updateRangeCircle(
                                selectedEntityRangeCircle!!,
                                newPosition.x.toFloat(),
                                newPosition.y.toFloat(),
                                newPosition.z.toFloat(),
                                currentRadius
                            )
                        }
                    }
                })
            }
        }
    }
    
    /**
     * Creates and displays the actual range circle mesh.
     */
    private fun createAndDisplayRangeCircle(entity: QuestObjectModel, radius: Float) {
        val position = entity.worldPosition.value
        val circle = rangeCircleRenderer.createRangeCircle(
            position.x.toFloat(),
            position.y.toFloat(),
            position.z.toFloat(),
            radius
        )
        renderContext.helpers.add(circle)
        selectedEntityRangeCircle = circle
    }
    
    /**
     * Recreates the range circle with a new radius without setting up new observers.
     */
    private fun recreateRangeCircle(entity: QuestObjectModel, radius: Float) {
        // Remove old circle without disposing observers
        selectedEntityRangeCircle?.let { circle ->
            renderContext.helpers.remove(circle)
            disposeObject3DResources(circle)
        }
        selectedEntityRangeCircle = null
        
        // Create new circle with updated radius
        if (radius > 0) {
            createAndDisplayRangeCircle(entity, radius)
        }
    }

    private data class TypeAndModel(val type: EntityType, val model: Int?)
}
