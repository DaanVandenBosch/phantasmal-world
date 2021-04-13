package world.phantasmal.web.questEditor.rendering.input.state

import world.phantasmal.web.core.minus
import world.phantasmal.web.externals.three.Mesh
import world.phantasmal.web.externals.three.Vector2
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.rendering.EntityInstancedMesh
import world.phantasmal.web.questEditor.rendering.input.*

class IdleState(
    private val ctx: StateContext,
    private val entityManipulationEnabled: Boolean,
) : State() {
    private var panning = false
    private var rotating = false
    private var zooming = false
    private val pointerDevicePosition = Vector2()
    private var shouldCheckHighlight = false

    override fun processEvent(event: Evt): State {
        when (event) {
            is KeyboardEvt -> {
                if (entityManipulationEnabled) {
                    val quest = ctx.quest.value
                    val entity = ctx.selectedEntity.value

                    if (quest != null && entity != null && event.key == "Delete") {
                        ctx.deleteEntity(quest, entity)
                    }
                }
            }

            is PointerDownEvt -> {
                val pick = pickEntity(event.pointerDevicePosition)

                when (event.buttons) {
                    1 -> {
                        if (pick == null) {
                            panning = true
                        } else {
                            ctx.setSelectedEntity(pick.entity)

                            if (entityManipulationEnabled) {
                                return TranslationState(
                                    ctx,
                                    pick.entity,
                                    pick.dragAdjust,
                                    pick.grabOffset,
                                )
                            }
                        }
                    }
                    2 -> {
                        if (pick == null) {
                            rotating = true
                        } else {
                            ctx.setSelectedEntity(pick.entity)

                            if (entityManipulationEnabled) {
                                return RotationState(
                                    ctx,
                                    pick.entity,
                                    pick.grabOffset,
                                )
                            }
                        }
                    }
                    4 -> {
                        zooming = true
                    }
                }
            }

            is PointerUpEvt -> {
                if (panning) {
                    updateCameraTarget()
                }

                panning = false
                rotating = false
                zooming = false

                // If the user clicks on nothing, deselect the currently selected entity.
                if (!event.movedSinceLastPointerDown &&
                    pickEntity(event.pointerDevicePosition) == null
                ) {
                    ctx.setSelectedEntity(null)
                    pickAndHighlightMesh()
                }
            }

            is PointerMoveEvt -> {
                if (!panning && !rotating && !zooming) {
                    // User is hovering.
                    pointerDevicePosition.copy(event.pointerDevicePosition)
                    shouldCheckHighlight = true
                }
            }

            is PointerOutEvt -> {
                ctx.setHighlightedEntity(null)
                shouldCheckHighlight = false
            }

            is EntityDragEnterEvt -> {
                val quest = ctx.quest.value
                val area = ctx.area.value

                if (quest != null && area != null) {
                    return CreationState(ctx, event, quest, area)
                }
            }

            else -> return this
        }

        return this
    }

    override fun beforeRender() {
        if (shouldCheckHighlight) {
            ctx.setHighlightedEntity(pickEntity(pointerDevicePosition)?.entity)
            shouldCheckHighlight = false
        }
    }

    override fun cancel() {
        // Do nothing.
    }

    private fun updateCameraTarget() {
        // If the user moved the camera, try setting the camera target to a better point.
        ctx.pickGround(ZERO_VECTOR_2)?.let { intersection ->
            ctx.cameraInputManager.setTarget(intersection.point)
        }
    }

    /**
     * @param pointerPosition pointer coordinates in normalized device space
     */
    private fun pickEntity(pointerPosition: Vector2): Pick? {
        // Find the nearest entity under the pointer.
        val intersection = ctx.intersectObject(
            pointerPosition,
            ctx.renderContext.entities,
        ) { it.`object`.visible }

        intersection ?: return null

        val entityInstancedMesh = intersection.`object`.userData
        val instanceIndex = intersection.instanceId

        if (instanceIndex == null || entityInstancedMesh !is EntityInstancedMesh) {
            return null
        }

        val entity = entityInstancedMesh.getInstanceAt(instanceIndex).entity
        val entityPosition = entity.worldPosition.value

        // Vector from the point where we grab the entity to its position.
        val grabOffset = entityPosition - intersection.point

        // Vector from the point where we grab the entity to the point on the ground right beneath
        // its position. The same as grabOffset when an entity is standing on the ground.
        val dragAdjust = grabOffset.clone()

        // Find vertical distance to the ground.
        ctx.intersectObject(
            origin = entityPosition,
            direction = DOWN_VECTOR,
            ctx.renderContext.collisionGeometry,
        )?.let { groundIntersection ->
            dragAdjust.y -= groundIntersection.distance
        }

        return Pick(entity, grabOffset, dragAdjust)
    }

    private fun pickAndHighlightMesh() {
        if (ctx.devMode.value) {
            val intersection = ctx.intersectObject(
                pointerDevicePosition,
                ctx.renderContext.renderGeometry,
            ) { it.`object`.visible }

            ctx.setHighlightedMesh(intersection?.`object` as Mesh?)
        } else {
            ctx.setHighlightedMesh(null)
        }
    }

    private class Pick(
        val entity: QuestEntityModel<*, *>,

        /**
         * Vector that points from the grabbing point (somewhere on the model's surface) to the entity's
         * origin.
         */
        val grabOffset: Vector3,

        /**
         * Vector that points from the grabbing point to the terrain point directly under the entity's
         * origin.
         */
        val dragAdjust: Vector3,
    )

    companion object {
        private val ZERO_VECTOR_2 = Vector2(0.0, 0.0)
        private val DOWN_VECTOR = Vector3(0.0, -1.0, 0.0)
    }
}
