package world.phantasmal.web.questEditor.rendering

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.khronos.webgl.Float32Array
import world.phantasmal.cell.observeNow
import world.phantasmal.core.disposable.DisposableSupervisedScope
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.fileFormats.quest.EntityType
import world.phantasmal.psolib.fileFormats.quest.ObjectType
import world.phantasmal.web.core.loading.LoadingCache
import world.phantasmal.web.core.rendering.disposeObject3DResources
import world.phantasmal.web.externals.three.*
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.models.*
import world.phantasmal.web.questEditor.stores.AreaStore
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.obj

private val logger = KotlinLogging.logger {}

class EntityMeshManager(
    private val questEditorStore: QuestEditorStore,
    private val renderContext: QuestRenderContext,
    private val entityAssetLoader: EntityAssetLoader,
    private val areaStore: AreaStore,
    private val enableRoomLabels: Boolean = false, // Only enable room labels for one manager
) : DisposableContainer() {
    private val scope = addDisposable(DisposableSupervisedScope(this::class, Dispatchers.Main))
    private val rangeCircleRenderer = RangeCircleRenderer()
    private val roomIdRenderer = RoomIdRenderer()

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

    /**
     * Room ID labels currently being displayed.
     */
    private val roomIdLabels = mutableMapOf<Int, Group>()
    
    /**
     * Disposer for room ID label observers.
     */
    private var roomIdObserverDisposer = addDisposable(Disposer())

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
        // Set up ground height calculator for NPCs
        QuestNpcModel.setGroundHeightCalculator { x, z, section ->
            calculateGroundHeight(x, z)
        }
        
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
        
        // Initialize room ID labels when quest, area, or show setting changes (only for enabled managers)
        if (enableRoomLabels) {
            observeNow(questEditorStore.currentQuest) { _ ->
                updateRoomIdLabels()
            }
            observeNow(questEditorStore.currentAreaVariant) { _ ->
                updateRoomIdLabels()
            }
            observeNow(questEditorStore.showRoomIds) { _ ->
                updateRoomIdLabels()
            }
        }
    }

    override fun dispose() {
        removeAll()
        renderContext.entities.clear()
        disposeObject3DResources(warpLines)
        // Dispose selected entity range circle
        clearSelectedEntityRangeCircle()
        // Dispose room ID labels (only for enabled managers)
        if (enableRoomLabels) {
            clearRoomIdLabels()
        }
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
                                newPosition.z.toFloat()
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

    /**
     * Updates room ID labels for the current area and sections.
     * Uses fixed map geometry data, independent of quest files.
     */
    private fun updateRoomIdLabels() {
        // Always clear existing labels first
        clearRoomIdLabels()
        
        // Check if room IDs should be shown
        if (!questEditorStore.showRoomIds.value) {
            return // Don't create any labels if room IDs are disabled
        }
        
        val currentAreaVariant = questEditorStore.currentAreaVariant.value ?: return
        val currentQuest = questEditorStore.currentQuest.value
        
        // Determine episode: use quest episode if available, otherwise default to Episode.I
        val episode = currentQuest?.episode ?: Episode.I
        
        // Check if the area variant already has sections loaded (from geometry)
        val sections = currentAreaVariant.sections.value
        if (sections.isNotEmpty()) {
            // Use already loaded sections immediately (synchronous)
            createRoomLabelsForSections(sections, currentAreaVariant)
        } else {
            // Sections not loaded yet, need to load them directly from area store
            scope.launch {
                try {
                    // Check if we're still on the same area variant when the coroutine executes
                    if (questEditorStore.currentAreaVariant.value != currentAreaVariant) {
                        return@launch
                    }
                    
                    // Load sections directly from area store - this is independent of quest files
                    val loadedSections = areaStore.getSections(episode, currentAreaVariant)
                    
                    // Double check we're still on the same area variant after loading
                    if (questEditorStore.currentAreaVariant.value != currentAreaVariant) {
                        return@launch
                    }
                    
                    // Update the area variant with the loaded sections
                    currentAreaVariant.setSections(loadedSections)
                    
                    // Create room labels with the loaded sections
                    if (loadedSections.isNotEmpty()) {
                        createRoomLabelsForSections(loadedSections, currentAreaVariant)
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to load sections for area variant ${currentAreaVariant.area.name}" }
                }
            }
        }
    }
    
    /**
     * Creates room labels for a list of sections.
     * Uses stable section data from map geometry.
     */
    private fun createRoomLabelsForSections(
        sections: List<SectionModel>, 
        areaVariant: AreaVariantModel,
    ) {
        for (section in sections) {
            // Always use section position from map geometry - this is the stable, fixed position
            val roomCenter = section.position
            
            // Use a unique key combining area variant and section for proper tracking
            val uniqueKey = "${areaVariant.area.id}_${areaVariant.id}_${section.id}".hashCode()
            
            // Display the section ID as it appears within this area
            createRoomIdLabelForSection(
                uniqueKey,
                section.id,  // Use the actual section ID within this area
                roomCenter.x.toFloat(),
                roomCenter.y.toFloat(),
                roomCenter.z.toFloat()
            )
        }
    }
    
    /**
     * Creates and displays a room ID label for a specific section with unique tracking.
     */
    private fun createRoomIdLabelForSection(uniqueKey: Int, roomId: Int, centerX: Float, centerY: Float, centerZ: Float) {
        val label = roomIdRenderer.createRoomIdLabel(centerX, centerY, centerZ, roomId)
        renderContext.helpers.add(label)
        roomIdLabels[uniqueKey] = label
    }
    
    /**
     * Clears all room ID labels and their observers.
     */
    private fun clearRoomIdLabels() {
        // Clear our tracked labels
        for (label in roomIdLabels.values) {
            renderContext.helpers.remove(label)
            renderContext.scene.remove(label)
            disposeObject3DResources(label)
        }
        roomIdLabels.clear()
        
        // Additional cleanup: remove any remaining room ID labels by name pattern
        val toRemove = mutableListOf<Object3D>()
        renderContext.helpers.children.forEach { child ->
            if (child.name.startsWith("RoomIdLabel_")) {
                toRemove.add(child)
            }
        }
        for (obj in toRemove) {
            renderContext.helpers.remove(obj)
            disposeObject3DResources(obj)
        }
        
        roomIdObserverDisposer.disposeAll()
    }

    private fun calculateGroundHeight(x: Double, z: Double): Double {
        // Create a raycaster from above the position downward to find ground
        val raycaster = Raycaster()
        val startHeight = 1000.0  // Start from high above
        val origin = Vector3(x, startHeight, z)
        val direction = Vector3(0.0, -1.0, 0.0)  // Downward

        raycaster.set(origin, direction)
        val intersections = arrayOf<Intersection>()
        raycaster.intersectObject(renderContext.collisionGeometry, recursive = true, intersections)

        // Find the first valid ground intersection
        val groundIntersection = intersections.find { intersection ->
            // Same logic as pickGround - don't allow steep terrain
            intersection.face?.normal?.let { n -> n.y > 0.75 } ?: false
        }

        return groundIntersection?.point?.y ?: 0.0
    }

    private data class TypeAndModel(val type: EntityType, val model: Int?)
}
