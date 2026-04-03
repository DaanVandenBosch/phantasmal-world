package world.phantasmal.web.questEditor.rendering

import mu.KotlinLogging
import world.phantasmal.cell.observeNow
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.psolib.fileFormats.quest.ObjectType
import world.phantasmal.web.core.rendering.disposeObject3DResources
import world.phantasmal.web.externals.three.Object3D
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.QuestEntityPropModel
import world.phantasmal.web.questEditor.models.QuestObjectModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.DisposableContainer

private val logger = KotlinLogging.logger {}

/**
 * Manages range circle, collision rectangle, and SCL_TAMA visualizations
 * for the currently selected entity.
 */
class SelectionVisualizationManager(
    private val questEditorStore: QuestEditorStore,
    private val renderContext: QuestRenderContext,
    private val sectionIdRenderer: SectionIdRenderer,
) : DisposableContainer() {
    private val rangeCircleRenderer = RangeCircleRenderer()
    private val collisionRectRenderer = CollisionRectRenderer()

    private var selectedEntityRangeCircle: Object3D? = null
    private var selectedEntityCollisionRect: Object3D? = null
    private var rangeCircleObserverDisposer = addDisposable(Disposer())

    /**
     * Updates visualizations for the currently selected entity.
     * Called by EntityMeshManager when selection changes.
     */
    fun updateForSelectedEntity(entity: QuestEntityModel<*, *>?) {
        clearAll()

        if (entity is QuestObjectModel && (entity.type == ObjectType.EventCollision || entity.type == ObjectType.ScriptCollision)) {
            createRangeCircleForEntity(entity)
        }

        if (entity is QuestObjectModel && entity.type == ObjectType.ObjRoomID) {
            createSclTamaCircleForSelectedEntity(entity)
        }

        if (entity is QuestObjectModel &&
            (entity.type == ObjectType.LaserFenceEx || entity.type == ObjectType.LaserSquareFenceEx)
        ) {
            createCollisionRectForEntity(entity)
        }
    }

    /**
     * Clears visualization if the given entity is currently selected.
     * Called when an entity is removed.
     */
    fun clearIfSelected(entity: QuestEntityModel<*, *>) {
        if (entity is QuestObjectModel && entity.type == ObjectType.ObjRoomID &&
            questEditorStore.selectedEntity.value == entity
        ) {
            clearAll()
        }
    }

    override fun dispose() {
        clearAll()
        super.dispose()
    }

    private fun clearAll() {
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

    // ---- Range circle (EventCollision / ScriptCollision) ----

    private fun createRangeCircleForEntity(entity: QuestObjectModel) {
        val radiusProp = entity.properties.value.find { it.name == "Radius" }
        if (radiusProp != null) {
            val radius = radiusProp.value.value as? Float ?: return
            if (radius > 0) {
                createAndDisplayRangeCircle(entity, radius)

                rangeCircleObserverDisposer.add(radiusProp.value.observeNow { newRadius ->
                    val newRadiusFloat = newRadius as? Float ?: return@observeNow
                    if (questEditorStore.selectedEntity.value == entity) {
                        if (newRadiusFloat > 0) {
                            recreateRangeCircle(entity, newRadiusFloat)
                        } else {
                            clearAll()
                        }
                    }
                })

                rangeCircleObserverDisposer.add(entity.worldPosition.observeNow { newPosition ->
                    if (questEditorStore.selectedEntity.value == entity && selectedEntityRangeCircle != null) {
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

    private fun recreateRangeCircle(entity: QuestObjectModel, radius: Float) {
        selectedEntityRangeCircle?.let { circle ->
            renderContext.helpers.remove(circle)
            disposeObject3DResources(circle)
        }
        selectedEntityRangeCircle = null

        if (radius > 0) {
            createAndDisplayRangeCircle(entity, radius)
        }
    }

    // ---- Collision rectangle (LaserFenceEx / LaserSquareFenceEx) ----

    private fun createCollisionRectForEntity(entity: QuestObjectModel) {
        val depthProp = entity.properties.value.find { it.name == "Collision depth" }
        val widthProp = entity.properties.value.find { it.name == "Collision width" }
        if (widthProp == null || depthProp == null) return

        val width = widthProp.value.value as? Float ?: return
        val depth = depthProp.value.value as? Float ?: return

        if (width > 0 && depth > 0) {
            createAndDisplayCollisionRect(entity, width, depth)
        }

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
            depth,
            width,
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

    // ---- SCL_TAMA (ObjRoomID) ----

    private fun createSclTamaCircleForSelectedEntity(entity: QuestObjectModel) {
        try {
            val sclTamaValue = entity.entity.data.getFloat(40)

            if (sclTamaValue > 0.0f) {
                val position = entity.worldPosition.value

                val visualization = sectionIdRenderer.createSclTamaVisualization(
                    position.x.toFloat(),
                    position.y.toFloat(),
                    position.z.toFloat(),
                    sclTamaValue
                )

                renderContext.helpers.add(visualization)
                selectedEntityRangeCircle = visualization

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
}
