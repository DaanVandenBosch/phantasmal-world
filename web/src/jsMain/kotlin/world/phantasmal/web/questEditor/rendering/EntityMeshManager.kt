package world.phantasmal.web.questEditor.rendering

import kotlinx.browser.window
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.khronos.webgl.Float32Array
import world.phantasmal.cell.map
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
    private val enableSectionLabels: Boolean = false, // Only enable section labels for one manager
) : DisposableContainer() {
    private val scope = addDisposable(DisposableSupervisedScope(this::class, Dispatchers.Main))
    private val rangeCircleRenderer = RangeCircleRenderer()
    private val collisionRectRenderer = CollisionRectRenderer()
    private val sectionIdRenderer = SectionIdRenderer()

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

    /**
     * Challenge mode random monster spawn points.
     */
    private val challengeMonsterSpawnContainer = addDisposable(
        ChallengeMonsterSpawnContainer().also {
            renderContext.helpers.add(it.mesh)  // Add to helpers, not entities (non-selectable)
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
     * Collision rectangle for the currently selected entity (LaserFenceEx / LaserSquareFenceEx).
     */
    private var selectedEntityCollisionRect: Object3D? = null

    /**
     * Disposer for selected entity range circle observers.
     */
    private var rangeCircleObserverDisposer = addDisposable(Disposer())

    /**
     * Section ID labels currently being displayed.
     */
    private val sectionIdLabels = mutableMapOf<Int, Group>()
    
    /**
     * Disposer for section ID label observers.
     */
    private var sectionIdObserverDisposer = addDisposable(Disposer())

    /**
     * Persistent Door ID labels displayed above door objects (like section ID circles).
     * During playback, matching doors show "Unlock N" / "Lock N" instead of "Door N".
     */
    private val doorIdLabels = mutableListOf<Group>()

    /** Active flash animation timer IDs — cleared together with doorIdLabels. */
    private val doorFlashTimers = mutableListOf<Int>()

    /**
     * SCL_TAMA circles currently being displayed for ObjSectionID objects.
     */
    private val sclTamaCircles = mutableMapOf<QuestObjectModel, Group>()

    /**
     * Playback action labels currently displayed above doors/entities during event playback.
     */
    private val playbackLabels = mutableListOf<Group>()

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

        // Initialize section ID labels when area, show setting, or selected section changes (only for enabled managers)
        // Note: We don't observe currentQuest because currentAreaVariant is set after currentQuest,
        // and observing currentQuest would cause us to read stale area variant data
        if (enableSectionLabels) {
            var sectionsDisposer: world.phantasmal.core.disposable.Disposable? = null

            observeNow(questEditorStore.currentAreaVariant) { areaVariant ->
                // Dispose previous sections observer
                sectionsDisposer?.dispose()
                sectionsDisposer = null

                updateSectionIdLabels()
                updateDoorIdLabels()

                // Also observe the sections list of the current area variant
                // This ensures labels are updated when sections are loaded from map files
                if (areaVariant != null) {
                    sectionsDisposer = areaVariant.sections.observeNow { sections ->
                        updateSectionIdLabels()
                        updateDoorIdLabels()
                    }
                }
            }
            observeNow(questEditorStore.showSectionIds) { _ ->
                updateSectionIdLabels()
            }
            observeNow(questEditorStore.showDoorIds) { _ ->
                updateDoorIdLabels()
            }
            observeNow(questEditorStore.selectedSection) { _ ->
                updateSectionIdLabels()
            }
        }

        // Only one manager should handle playback labels to avoid duplicates.
        if (enableSectionLabels) {
            observeNow(
                map(
                    questEditorStore.playbackDoorIds,
                    questEditorStore.playbackLockDoorIds,
                    questEditorStore.playbackSpawnSectionIds,
                ) { unlock, lock, spawn -> Triple(unlock, lock, spawn) }
            ) { (_, _, spawnSectionIds) ->
                // Door labels are rebuilt inline — matching doors swap to
                // "Unlock N" / "Lock N" with a flash effect.
                updateDoorIdLabels()
                // Spawn labels are separate overlays at section centres.
                updateSpawnLabels(spawnSectionIds)
            }
        }

        // Observe quest, area, and area variant changes to update challenge mode spawn points
        // Wait for area variant to match current area before creating spawns
        observeNow(
            map(
                questEditorStore.currentQuest,
                questEditorStore.currentArea,
                questEditorStore.currentAreaVariant,
            ) { quest, area, areaVariant ->
                Triple(quest, area, areaVariant)
            }
        ) { (quest, area, _) ->
            // updateChallengeMonsterSpawns will validate that areaVariant matches area
            // and skip spawn creation if they don't match yet
            updateChallengeMonsterSpawns(quest, area)
        }
    }

    override fun dispose() {
        removeAll()
        renderContext.entities.clear()
        disposeObject3DResources(warpLines)
        // Dispose selected entity range circle
        clearSelectedEntityRangeCircle()
        // Dispose section ID labels, door ID labels, and playback labels (only for enabled managers)
        if (enableSectionLabels) {
            clearSectionIdLabels()
            clearDoorIdLabels()
            clearPlaybackLabels()
        }
        // Dispose all SCL_TAMA circles
        clearAllSclTamaCircles()
        // Dispose challenge mode spawn container
        challengeMonsterSpawnContainer.clearInstances()
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
        
        // Remove SCL_TAMA circle if it exists
        if (entity is QuestObjectModel && entity.type == ObjectType.ObjRoomID) {
            removeSclTamaCircleForObjRoomId(entity)
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
        // Note: Don't clear challengeMonsterSpawnContainer here - it's managed by updateChallengeMonsterSpawns()
        // which observes area changes directly. Clearing it here causes spawns to disappear when NPCs are loaded.
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
     * Shows range circle for selected EventCollision and ScriptCollision objects (with bolder line for ScriptCollision),
     * and SCL_TAMA circle for ObjRoomID objects. All objects are processed based on their sectionId.
     */
    private fun updateSelectedEntityRangeCircle(entity: QuestEntityModel<*, *>?) {
        // Clear existing range circle and observers
        clearSelectedEntityRangeCircle()

        // Add range circle for selected EventCollision or ScriptCollision objects
        if (entity is QuestObjectModel && (entity.type == ObjectType.EventCollision || entity.type == ObjectType.ScriptCollision)) {
            createRangeCircleForEntity(entity)
        }
        
        // Add SCL_TAMA circle for selected ObjRoomID object
        if (entity is QuestObjectModel && entity.type == ObjectType.ObjRoomID) {
            createSclTamaCircleForSelectedEntity(entity)
        }

        // Add collision rectangle for selected LaserFenceEx or LaserSquareFenceEx objects
        if (entity is QuestObjectModel &&
            (entity.type == ObjectType.LaserFenceEx || entity.type == ObjectType.LaserSquareFenceEx)
        ) {
            createCollisionRectForEntity(entity)
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
        selectedEntityCollisionRect?.let { rect ->
            renderContext.helpers.remove(rect)
            disposeObject3DResources(rect)
        }
        selectedEntityCollisionRect = null
        rangeCircleObserverDisposer.disposeAll()
    }
    
    /**
     * Creates and displays a range circle for the specified EventCollision or ScriptCollision entity.
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
        val makeBolder = entity.type == ObjectType.ScriptCollision
        val circle = rangeCircleRenderer.createRangeCircle(
            position.x.toFloat(),
            position.y.toFloat(),
            position.z.toFloat(),
            radius,
            makeBolder = makeBolder
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
     * Creates and displays a collision rectangle for a LaserFenceEx or LaserSquareFenceEx entity.
     */
    private fun createCollisionRectForEntity(entity: QuestObjectModel) {
        val widthProp = entity.properties.value.find { it.name == "Collision width" }
        val depthProp = entity.properties.value.find { it.name == "Collision depth" }
        if (widthProp == null || depthProp == null) return

        val width = widthProp.value.value as? Float ?: return
        val depth = depthProp.value.value as? Float ?: return

        if (width > 0 && depth > 0) {
            createAndDisplayCollisionRect(entity, width, depth)
        }

        // Observe property changes to recreate rect
        rangeCircleObserverDisposer.add(widthProp.value.observeNow { _ ->
            if (questEditorStore.selectedEntity.value == entity) {
                recreateCollisionRect(entity, widthProp, depthProp)
            }
        })
        rangeCircleObserverDisposer.add(depthProp.value.observeNow { _ ->
            if (questEditorStore.selectedEntity.value == entity) {
                recreateCollisionRect(entity, widthProp, depthProp)
            }
        })

        // Observe position and rotation changes
        rangeCircleObserverDisposer.add(entity.worldPosition.observeNow { pos ->
            if (questEditorStore.selectedEntity.value == entity && selectedEntityCollisionRect != null) {
                val rot = entity.rotation.value
                collisionRectRenderer.updateCollisionRect(
                    selectedEntityCollisionRect!!,
                    pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat(),
                    rot.y.toFloat(),
                )
            }
        })
        rangeCircleObserverDisposer.add(entity.rotation.observeNow { rot ->
            if (questEditorStore.selectedEntity.value == entity && selectedEntityCollisionRect != null) {
                val pos = entity.worldPosition.value
                collisionRectRenderer.updateCollisionRect(
                    selectedEntityCollisionRect!!,
                    pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat(),
                    rot.y.toFloat(),
                )
            }
        })
    }

    private fun createAndDisplayCollisionRect(
        entity: QuestObjectModel,
        width: Float,
        depth: Float,
    ) {
        val position = entity.worldPosition.value
        val rotation = entity.rotation.value
        val rect = collisionRectRenderer.createCollisionRect(
            position.x.toFloat(),
            position.y.toFloat(),
            position.z.toFloat(),
            width,
            depth,
            rotation.y.toFloat(),
        )
        renderContext.helpers.add(rect)
        selectedEntityCollisionRect = rect
    }

    private fun recreateCollisionRect(
        entity: QuestObjectModel,
        widthProp: QuestEntityPropModel,
        depthProp: QuestEntityPropModel,
    ) {
        selectedEntityCollisionRect?.let { rect ->
            renderContext.helpers.remove(rect)
            disposeObject3DResources(rect)
        }
        selectedEntityCollisionRect = null

        val width = widthProp.value.value as? Float ?: return
        val depth = depthProp.value.value as? Float ?: return
        if (width > 0 && depth > 0) {
            createAndDisplayCollisionRect(entity, width, depth)
        }
    }

    /**
     * Updates section ID labels for the current area and sections.
     * Uses fixed map geometry data, independent of quest files.
     */
    private fun updateSectionIdLabels() {
        // Always clear existing labels first
        clearSectionIdLabels()

        // Check if section IDs should be shown
        if (!questEditorStore.showSectionIds.value) {
            return // Don't create any labels if section IDs are disabled
        }
        
        val currentAreaVariant = questEditorStore.currentAreaVariant.value ?: return
        val currentQuest = questEditorStore.currentQuest.value
        
        // Determine episode: use quest episode if available, otherwise default to Episode.I
        val episode = currentQuest?.episode ?: Episode.I
        
        // Check if the area variant already has sections loaded (from geometry)
        val sections = currentAreaVariant.sections.value
        if (sections.isNotEmpty()) {
            // Use already loaded sections immediately (synchronous)
            createSectionLabelsForSections(sections, currentAreaVariant)
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

                    // Create section labels with the loaded sections
                    if (loadedSections.isNotEmpty()) {
                        createSectionLabelsForSections(loadedSections, currentAreaVariant)
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to load sections for area variant ${currentAreaVariant.area.name}" }
                }
            }
        }
    }
    
    /**
     * Creates section labels for a list of sections.
     * Uses stable section data from map geometry.
     */
    private fun createSectionLabelsForSections(
        sections: List<SectionModel>,
        areaVariant: AreaVariantModel,
    ) {
        for (section in sections) {
            // Always use section position from map geometry - this is the stable, fixed position
            val sectionCenter = section.position

            // Use a unique key combining area variant and section for proper tracking
            val uniqueKey = "${areaVariant.area.id}_${areaVariant.id}_${section.id}".hashCode()

            // Display the section ID as it appears within this area
            createSectionIdLabelForSection(
                uniqueKey,
                section.id,  // Use the actual section ID within this area
                sectionCenter.x.toFloat(),
                sectionCenter.y.toFloat(),
                sectionCenter.z.toFloat()
            )
        }
    }
    
    /**
     * Creates and displays a section ID label for a specific section with unique tracking.
     * Uses different visual style for selected section.
     */
    private fun createSectionIdLabelForSection(
        uniqueKey: Int,
        sectionId: Int,
        centerX: Float,
        centerY: Float,
        centerZ: Float
    ) {
        // Check if this section is currently selected
        val selectedSection = questEditorStore.selectedSection.value
        val isSelected = selectedSection?.id == sectionId

        val label = if (isSelected) {
            sectionIdRenderer.createSelectedSectionIdLabel(centerX, centerY, centerZ, sectionId)
        } else {
            sectionIdRenderer.createSectionIdLabel(centerX, centerY, centerZ, sectionId)
        }
        renderContext.helpers.add(label)
        sectionIdLabels[uniqueKey] = label
    }
    
    /**
     * Clears all section ID labels and their observers.
     */
    private fun clearSectionIdLabels() {
        // Clear our tracked labels
        for (label in sectionIdLabels.values) {
            renderContext.helpers.remove(label)
            renderContext.scene.remove(label)
            disposeObject3DResources(label)
        }
        sectionIdLabels.clear()

        // Additional cleanup: remove any remaining section ID labels by name pattern
        val toRemove = mutableListOf<Object3D>()
        renderContext.helpers.children.forEach { child ->
            if (child.name.startsWith("SectionIdLabel_") || child.name.startsWith("SelectedSectionIdLabel_")) {
                toRemove.add(child)
            }
        }
        for (obj in toRemove) {
            renderContext.helpers.remove(obj)
            disposeObject3DResources(obj)
        }

        sectionIdObserverDisposer.disposeAll()
    }

    /**
     * Updates Door ID labels above every door object in the current area.
     * During playback, matching doors show "Unlock N" / "Lock N" (with a flash)
     * instead of the default "Door N".
     */
    private fun updateDoorIdLabels() {
        clearDoorIdLabels()

        if (!questEditorStore.showDoorIds.value) return

        val quest = questEditorStore.currentQuest.value ?: return
        val area = questEditorStore.currentArea.value ?: return
        val areaVariant = questEditorStore.currentAreaVariant.value

        val unlockIds = questEditorStore.playbackDoorIds.value
        val lockIds = questEditorStore.playbackLockDoorIds.value

        for (obj in quest.objects.value) {
            val belongsToCurrentArea = if (quest.floorMappings.isNotEmpty() && areaVariant != null) {
                val mapping = quest.floorMappings.find { it.floorId == obj.areaId }
                mapping?.areaId == area.id && mapping.variantId == areaVariant.id
            } else {
                obj.areaId == area.id
            }
            if (!belongsToCurrentArea) continue

            if (obj.type !in doorObjectTypes) continue
            val doorIdValue = getEffectiveDoorId(obj)
            if (doorIdValue < 0) continue

            val switchAmount = getSwitchAmount(obj)
            val doorIds = (doorIdValue until doorIdValue + switchAmount).toSet()
            val matchedUnlockIds = doorIds.intersect(unlockIds)
            val matchedLockIds = doorIds.intersect(lockIds)
            val isAction = matchedUnlockIds.isNotEmpty() || matchedLockIds.isNotEmpty()

            val isFence = obj.type in fenceObjectTypes
            val rangeStr =
                if (switchAmount > 1) "$doorIdValue-${doorIdValue + switchAmount - 1}"
                else "$doorIdValue"
            val defaultLabel = if (isFence) "Fence $rangeStr" else "Door $rangeStr"

            val (text, color) = when {
                matchedUnlockIds.isNotEmpty() -> {
                    val ids = matchedUnlockIds.sorted().joinToString(",")
                    "Unlock $ids" to SectionIdRenderer.UNLOCK_COLOR
                }
                matchedLockIds.isNotEmpty() -> {
                    val ids = matchedLockIds.sorted().joinToString(",")
                    "Lock $ids" to SectionIdRenderer.LOCK_COLOR
                }
                else -> defaultLabel to SectionIdRenderer.DOOR_ID_COLOR
            }

            val pos = obj.worldPosition.value
            val label = sectionIdRenderer.createDoorLabel(
                pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat(),
                text, color,
            )
            renderContext.helpers.add(label)
            doorIdLabels.add(label)

            if (isAction) {
                flashLabel(label)
            }
        }
    }

    /**
     * Clears all Door ID labels and cancels running flash animations.
     */
    private fun clearDoorIdLabels() {
        for (id in doorFlashTimers) window.clearTimeout(id)
        doorFlashTimers.clear()
        for (label in doorIdLabels) {
            renderContext.helpers.remove(label)
            disposeObject3DResources(label)
        }
        doorIdLabels.clear()
    }

    /** Blinks a label twice so the user notices the state change. */
    private fun flashLabel(label: Group) {
        val step = 150
        for (i in 1..4) {
            doorFlashTimers.add(window.setTimeout({
                label.visible = i % 2 == 0
            }, step * i))
        }
        // Guarantee visible after the last toggle.
        doorFlashTimers.add(window.setTimeout({ label.visible = true }, step * 5))
    }

    /**
     * Creates SCL_TAMA visualization for the selected ObjRoomID object.
     * Shows circle, radius lines, and value display only when the entity is selected.
     */
    private fun createSclTamaCircleForSelectedEntity(entity: QuestObjectModel) {
        try {
            // Get SCL_TAMA value from entity data (offset 40, float)
            val sclTamaValue = entity.entity.data.getFloat(40)
            
            // Only create visualization if SCL_TAMA value is positive
            if (sclTamaValue > 0.0f) {
                val position = entity.worldPosition.value

                val visualization = sectionIdRenderer.createSclTamaVisualization(
                    position.x.toFloat(),
                    position.y.toFloat(),
                    position.z.toFloat(),
                    sclTamaValue
                )
                
                // Add to helpers group (not entities, since this is visual aid, not selectable)
                renderContext.helpers.add(visualization)
                
                // Store as selected entity range circle for proper cleanup
                selectedEntityRangeCircle = visualization
                
                // Observe position changes to update the visualization position
                rangeCircleObserverDisposer.add(entity.worldPosition.observeNow { newPosition ->
                    if (questEditorStore.selectedEntity.value == entity && selectedEntityRangeCircle != null) {
                        selectedEntityRangeCircle!!.position.set(
                            newPosition.x, 
                            newPosition.y, 
                            newPosition.z
                        )
                    }
                })
                
                logger.debug { "Created SCL_TAMA visualization for selected ObjRoomID at (${position.x}, ${position.y}, ${position.z}) with radius ${sclTamaValue * 10.0f}" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to create SCL_TAMA visualization for selected ObjRoomID entity" }
        }
    }

    /**
     * Removes and disposes the SCL_TAMA circle for the specified ObjRoomID entity.
     */
    private fun removeSclTamaCircleForObjRoomId(entity: QuestObjectModel) {
        sclTamaCircles.remove(entity)?.let { circle ->
            renderContext.entities.remove(circle)
            disposeObject3DResources(circle)
            logger.debug { "Removed SCL_TAMA circle for ObjRoomID entity" }
        }
    }
    
    /**
     * Clears all SCL_TAMA circles.
     */
    private fun clearAllSclTamaCircles() {
        for (circle in sclTamaCircles.values) {
            renderContext.entities.remove(circle)
            disposeObject3DResources(circle)
        }
        sclTamaCircles.clear()
        logger.debug { "Cleared all SCL_TAMA circles" }
    }

    /**
     * Subset of doorObjectTypes that are laser fences (shown as "Fence N" instead of "Door N").
     */
    private val fenceObjectTypes: Set<ObjectType> = setOf(
        ObjectType.LaserFence,
        ObjectType.LaserSquareFence,
        ObjectType.ForestLaserFenceSwitch,
        ObjectType.LaserFenceEx,
        ObjectType.LaserSquareFenceEx,
        ObjectType.RuinsLaserFence4x2,
        ObjectType.RuinsLaserFence6x2,
        ObjectType.RuinsLaserFence4x4,
        ObjectType.RuinsLaserFence6x4,
    )

    /**
     * All ObjectTypes whose value at data offset 52 acts as a door/switch ID
     * that the event system's unlock/lock actions can reference.
     */
    private val doorObjectTypes: Set<ObjectType> = setOf(
        // Forest
        ObjectType.ForestDoor,
        ObjectType.EnergyBarrier,
        ObjectType.ForestRisingBridge,
        // Caves
        ObjectType.Caves4ButtonDoor,
        ObjectType.CavesNormalDoor,
        ObjectType.CavesSwitchDoor,
        // Mines
        ObjectType.MinesDoor,
        ObjectType.MinesSwitchDoor,
        // Ruins
        ObjectType.Ruins1Door,
        ObjectType.Ruins2Door,
        ObjectType.Ruins3Door,
        ObjectType.Ruins11ButtonDoor,
        ObjectType.Ruins21ButtonDoor,
        ObjectType.Ruins31ButtonDoor,
        ObjectType.Ruins4ButtonDoor,
        ObjectType.Ruins2ButtonDoor,
        // Laser fences — use "Switch ID" at offset 52
        ObjectType.LaserFence,
        ObjectType.LaserSquareFence,
        ObjectType.ForestLaserFenceSwitch,
        ObjectType.LaserFenceEx,
        ObjectType.LaserSquareFenceEx,
        ObjectType.RuinsLaserFence4x2,
        ObjectType.RuinsLaserFence6x2,
        ObjectType.RuinsLaserFence4x4,
        ObjectType.RuinsLaserFence6x4,
        // Ep2 — property is named "Switch ID" but the event system treats it the same
        ObjectType.SpaceshipDoor,
        ObjectType.TempleNormalDoor,
        ObjectType.CcaDoor,
        ObjectType.SeabedDoorWithBlueEdges,
    )

    /**
     * Extracts the effective door ID from an object at data offset 52.
     * ForestDoor stores the door ID in the lower byte only; other doors use the full int.
     * Returns -1 when the raw value is -1 (unassigned).
     */
    private fun getEffectiveDoorId(obj: QuestObjectModel): Int {
        val raw = obj.entity.data.getInt(52)
        if (raw == -1) return -1
        return if (obj.type == ObjectType.ForestDoor) raw and 0xFF else raw
    }

    /**
     * Returns the switch amount for doors that support multi-unlock. A door with
     * switch ID N and amount M requires unlocks N, N+1, …, N+M-1 to open.
     * Returns 1 for all other door types.
     */
    private fun getSwitchAmount(obj: QuestObjectModel): Int {
        // Fixed amount based on door type (no configurable property).
        if (obj.type == ObjectType.Ruins4ButtonDoor) return 4
        if (obj.type == ObjectType.Ruins2ButtonDoor) return 2

        // Configurable amount stored at data offset 56.
        // Fall back to type-implied default when the value is ≤ 0.
        if (obj.type in configurableSwitchDoorTypes) {
            val amount = obj.entity.data.getInt(56)
            if (amount > 1) return amount
            return configurableSwitchDefaults[obj.type] ?: 1
        }
        return 1
    }

    /**
     * Door types that store a switch amount/total at data offset 56,
     * meaning they require multiple sequential switch IDs to open.
     */
    private val configurableSwitchDoorTypes: Set<ObjectType> = setOf(
        ObjectType.Caves4ButtonDoor,
        ObjectType.MinesDoor,
        ObjectType.MinesSwitchDoor,
        ObjectType.CcaDoor,
        ObjectType.SeabedDoorWithBlueEdges,
    )

    /**
     * Default switch count for configurable multi-switch doors when the
     * stored value at offset 56 is ≤ 0 (not explicitly configured).
     */
    private val configurableSwitchDefaults: Map<ObjectType, Int> = mapOf(
        ObjectType.Caves4ButtonDoor to 4,
    )

    /**
     * Updates spawn labels at section centres during event playback.
     */
    private fun updateSpawnLabels(spawnSectionIds: Set<Int>) {
        clearPlaybackLabels()

        if (spawnSectionIds.isEmpty()) return

        val quest = questEditorStore.currentQuest.value ?: return
        val areaVariant = questEditorStore.currentAreaVariant.value

        val episode = quest.episode
        val sections = areaVariant?.let { areaStore.getLoadedSections(episode, it) } ?: emptyList()

        for (section in sections) {
            if (section.id in spawnSectionIds) {
                val pos = section.position
                val label = sectionIdRenderer.createPlaybackActionLabel(
                    pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat(),
                    "Spawn", SectionIdRenderer.SPAWN_COLOR,
                )
                renderContext.helpers.add(label)
                playbackLabels.add(label)
            }
        }
    }

    /**
     * Clears spawn playback labels.
     */
    private fun clearPlaybackLabels() {
        for (label in playbackLabels) {
            renderContext.helpers.remove(label)
            disposeObject3DResources(label)
        }
        playbackLabels.clear()
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

    /**
     * Updates challenge mode random monster spawn point visualizations for the current area.
     */
    private fun updateChallengeMonsterSpawns(quest: QuestModel?, area: AreaModel?) {
        // Clear existing spawn points
        challengeMonsterSpawnContainer.clearInstances()

        if (quest == null || area == null) {
            return
        }

        // Check if quest has challenge mode data
        if (quest.cmRandomSpawns.value.isEmpty()) {
            return
        }

        // Get the currently displayed area variant
        // IMPORTANT: Only use sections from the current variant to ensure correct coordinate transformation
        val areaVariant = questEditorStore.currentAreaVariant.value
        if (areaVariant == null) {
            logger.debug { "Area variant not available, skipping CM spawn creation" }
            return
        }

        // Validate that area variant matches the current area
        if (areaVariant.area.id != area.id) {
            // Area variant hasn't updated yet, skip spawn creation for now
            return
        }

        val sections = areaVariant.sections.value
        var totalSpawnsCreated = 0
        var spawnsSkipped = 0

        // Find spawn configurations for the current area
        for (cmSpawn in quest.cmRandomSpawns.value) {
            // Check if this spawn's areaId matches the current area/variant
            // Similar to how quest entities use floor mappings to determine their variant
            val shouldProcessSpawn = if (quest.floorMappings.isNotEmpty()) {
                // If quest has floor mappings, treat cmSpawn.areaId as a floor ID
                val floorMapping = quest.floorMappings.find { it.floorId == cmSpawn.areaId }
                floorMapping?.areaId == area.id && floorMapping.variantId == areaVariant.id
            } else {
                // Otherwise, treat cmSpawn.areaId as an area ID
                cmSpawn.areaId == area.id
            }

            if (shouldProcessSpawn) {
                // Create spawn point models for each entry
                for (entry in cmSpawn.entries) {
                    val sectionId = entry.sectionId.toInt()

                    // Find section in the currently displayed variant
                    val section = sections.find { it.id == sectionId }

                    if (section == null) {
                        // Section not in current variant
                        logger.debug { "CM spawn section $sectionId not found in variant ${areaVariant.id} of area ${area.id}" }
                        spawnsSkipped++
                        continue
                    }

                    val spawnModel = ChallengeMonsterSpawnModel(cmSpawn.areaId, entry, section)
                    challengeMonsterSpawnContainer.addInstance(spawnModel)
                    totalSpawnsCreated++
                }
            }
        }

        if (spawnsSkipped > 0) {
            logger.debug { "CM spawns in variant ${areaVariant.id}: $totalSpawnsCreated shown, $spawnsSkipped skipped" }
        }
    }

    /**
     * Called before each render to update text scales for constant screen size.
     */
    fun beforeRender() {
        if (enableSectionLabels) {
            // Update text scales for section ID labels
            if (sectionIdLabels.isNotEmpty()) {
                sectionIdRenderer.updateTextScales(renderContext.camera, sectionIdLabels.values)
            }
            // Update text scales for door ID labels
            if (doorIdLabels.isNotEmpty()) {
                sectionIdRenderer.updateTextScales(renderContext.camera, doorIdLabels)
            }
            // Update text scales for playback labels
            if (playbackLabels.isNotEmpty()) {
                sectionIdRenderer.updateTextScales(renderContext.camera, playbackLabels)
            }
        }
    }

    private data class TypeAndModel(val type: EntityType, val model: Int?)
}
