package world.phantasmal.web.questEditor.rendering

import kotlinx.browser.document
import mu.KotlinLogging
import org.w3c.dom.pointerevents.PointerEvent
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.web.core.minus
import world.phantasmal.web.core.plusAssign
import world.phantasmal.web.core.times
import world.phantasmal.web.externals.babylon.*
import world.phantasmal.web.questEditor.actions.TranslateEntityAction
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.SectionModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.dom.disposableListener

private val logger = KotlinLogging.logger {}

private val ZERO_VECTOR = Vector3.Zero()
private val DOWN_VECTOR = Vector3.Down()

class UserInputManager(
    questEditorStore: QuestEditorStore,
    private val renderer: QuestRenderer,
) : DisposableContainer() {
    private val stateContext = StateContext(questEditorStore, renderer)
    private val pointerPosition = Vector2.Zero()
    private val lastPointerPosition = Vector2.Zero()
    private var movedSinceLastPointerDown = false
    private var state: State
    private var onPointerUpListener: Disposable? = null
    private var onPointerMoveListener: Disposable? = null

    /**
     * Whether entity transformations, deletions, etc. are enabled or not.
     * Hover over and selection still work when this is set to false.
     */
    var entityManipulationEnabled: Boolean = true
        set(enabled) {
            field = enabled
            state.cancel()
            state = IdleState(stateContext, enabled)
        }

    init {
        state = IdleState(stateContext, entityManipulationEnabled)

        observe(questEditorStore.selectedEntity) { state.cancel() }

        addDisposables(
            disposableListener(renderer.canvas, "pointerdown", ::onPointerDown)
        )

        onPointerMoveListener = disposableListener(document, "pointermove", ::onPointerMove)
    }

    override fun internalDispose() {
        onPointerUpListener?.dispose()
        onPointerMoveListener?.dispose()
        super.internalDispose()
    }

    private fun onPointerDown(e: PointerEvent) {
        processPointerEvent(e)

        state = state.processEvent(
            PointerDownEvt(
                e.buttons.toInt(),
                shiftKeyDown = e.shiftKey,
                movedSinceLastPointerDown,
            )
        )

        onPointerUpListener = disposableListener(document, "pointerup", ::onPointerUp)

        // Stop listening to canvas move events and start listening to document move events.
        onPointerMoveListener?.dispose()
        onPointerMoveListener = disposableListener(document, "pointermove", ::onPointerMove)
    }

    private fun onPointerUp(e: PointerEvent) {
        try {
            processPointerEvent(e)

            state = state.processEvent(
                PointerUpEvt(
                    e.buttons.toInt(),
                    shiftKeyDown = e.shiftKey,
                    movedSinceLastPointerDown,
                )
            )
        } finally {
            onPointerUpListener?.dispose()
            onPointerUpListener = null

            // Stop listening to document move events and start listening to canvas move events.
            onPointerMoveListener?.dispose()
            onPointerMoveListener =
                disposableListener(renderer.canvas, "pointermove", ::onPointerMove)
        }
    }

    private fun onPointerMove(e: PointerEvent) {
        processPointerEvent(e)

        state = state.processEvent(
            PointerMoveEvt(
                e.buttons.toInt(),
                shiftKeyDown = e.shiftKey,
                movedSinceLastPointerDown,
            )
        )
    }

    private fun processPointerEvent(e: PointerEvent) {
        val rect = renderer.canvas.getBoundingClientRect()
        pointerPosition.set(e.clientX - rect.left, e.clientY - rect.top)

        when (e.type) {
            "pointerdown" -> {
                movedSinceLastPointerDown = false
            }
            "pointermove", "pointerup" -> {
                if (!pointerPosition.equals(lastPointerPosition)) {
                    movedSinceLastPointerDown = true
                }
            }
        }

        lastPointerPosition.copyFrom(pointerPosition)
    }
}

private class StateContext(
    private val questEditorStore: QuestEditorStore,
    val renderer: QuestRenderer,
) {
    private val plane = Plane.FromPositionAndNormal(Vector3.Up(), Vector3.Up())
    private val ray = Ray.Zero()

    val scene = renderer.scene

    fun setSelectedEntity(entity: QuestEntityModel<*, *>?) {
        questEditorStore.setSelectedEntity(entity)
    }

    fun translate(
        entity: QuestEntityModel<*, *>,
        dragAdjust: Vector3,
        grabOffset: Vector3,
        vertically: Boolean,
    ) {
        if (vertically) {
            // TODO: Vertical translation.
        } else {
            translateEntityHorizontally(entity, dragAdjust, grabOffset)
        }
    }

    fun finalizeTranslation(
        entity: QuestEntityModel<*, *>,
        newSection: SectionModel?,
        oldSection: SectionModel?,
        newPosition: Vector3,
        oldPosition: Vector3,
        world: Boolean,
    ) {
        questEditorStore.executeAction(TranslateEntityAction(
            ::setSelectedEntity,
            entity,
            newSection,
            oldSection,
            newPosition,
            oldPosition,
            world,
        ))
    }

    /**
     * If the drag-adjusted pointer is over the ground, translate an entity horizontally across the
     * ground. Otherwise translate the entity over the horizontal plane that intersects its origin.
     */
    private fun translateEntityHorizontally(
        entity: QuestEntityModel<*, *>,
        dragAdjust: Vector3,
        grabOffset: Vector3,
    ) {
        val pick = pickGround(scene.pointerX, scene.pointerY, dragAdjust)

        if (pick == null) {
            // If the pointer is not over the ground, we translate the entity across the horizontal
            // plane in which the entity's origin lies.
            scene.createPickingRayToRef(
                scene.pointerX,
                scene.pointerY,
                Matrix.IdentityReadOnly,
                ray,
                renderer.camera
            )

            plane.d = -entity.worldPosition.value.y + grabOffset.y

            ray.intersectsPlane(plane)?.let { distance ->
                // Compute the intersection point.
                val pos = ray.direction * distance
                pos += ray.origin
                // Compute the entity's new world position.
                pos.x += grabOffset.x
                pos.y = entity.worldPosition.value.y
                pos.z += grabOffset.z

                entity.setWorldPosition(pos)
            }
        } else {
            // TODO: Set entity section.
            entity.setWorldPosition(
                Vector3(
                    pick.pickedPoint!!.x,
                    pick.pickedPoint.y + grabOffset.y - dragAdjust.y,
                    pick.pickedPoint.z,
                )
            )
        }
    }

    fun pickGround(x: Double, y: Double, dragAdjust: Vector3 = ZERO_VECTOR): PickingInfo? {
        scene.createPickingRayToRef(
            x,
            y,
            Matrix.IdentityReadOnly,
            ray,
            renderer.camera
        )

        ray.origin += dragAdjust

        val pickingInfoArray = scene.multiPickWithRay(
            ray,
            { it.isEnabled() && it.metadata is CollisionMetadata },
        )

        if (pickingInfoArray != null) {
            for (pickingInfo in pickingInfoArray) {
                pickingInfo.getNormal()?.let { n ->
                    // Don't allow entities to be placed on very steep terrain. E.g. walls.
                    // TODO: make use of the flags field in the collision data.
                    if (n.y > 0.75) {
                        return pickingInfo
                    }
                }
            }
        }

        return null
    }
}

private sealed class Evt

private sealed class PointerEvt : Evt() {
    abstract val buttons: Int
    abstract val shiftKeyDown: Boolean
    abstract val movedSinceLastPointerDown: Boolean
}

private class PointerDownEvt(
    override val buttons: Int,
    override val shiftKeyDown: Boolean,
    override val movedSinceLastPointerDown: Boolean,
) : PointerEvt()

private class PointerUpEvt(
    override val buttons: Int,
    override val shiftKeyDown: Boolean,
    override val movedSinceLastPointerDown: Boolean,
) : PointerEvt()

private class PointerMoveEvt(
    override val buttons: Int,
    override val shiftKeyDown: Boolean,
    override val movedSinceLastPointerDown: Boolean,
) : PointerEvt()

private class Pick(
    val entity: QuestEntityModel<*, *>,
    val mesh: AbstractMesh,

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

private abstract class State {
    init {
        logger.trace { "Transitioning to ${this::class.simpleName}." }
    }

    abstract fun processEvent(event: Evt): State

    /**
     * The state object should stop doing what it's doing and revert to the idle state as soon as
     * possible.
     */
    abstract fun cancel()
}

private class IdleState(
    private val ctx: StateContext,
    private val entityManipulationEnabled: Boolean,
) : State() {
    override fun processEvent(event: Evt): State {
        when (event) {
            is PointerDownEvt -> {
                pickEntity()?.let { pick ->
                    when (event.buttons) {
                        1 -> {
                            ctx.setSelectedEntity(pick.entity)

                            if (entityManipulationEnabled) {
                                return TranslationState(
                                    ctx,
                                    pick.entity,
                                    pick.dragAdjust,
                                    pick.grabOffset
                                )
                            }
                        }
                        2 -> {
                            ctx.setSelectedEntity(pick.entity)

                            if (entityManipulationEnabled) {
                                // TODO: Enter RotationState.
                            }
                        }
                    }
                }
            }

            is PointerUpEvt -> {
                updateCameraTarget()

                // If the user clicks on nothing, deselect the currently selected entity.
                if (!event.movedSinceLastPointerDown && pickEntity() == null) {
                    ctx.setSelectedEntity(null)
                }
            }

            else -> {
                // Do nothing.
            }
        }

        return this
    }

    override fun cancel() {
        // Do nothing.
    }

    private fun updateCameraTarget() {
        // If the user moved the camera, try setting the camera
        // target to a better point.
        ctx.pickGround(
            ctx.renderer.engine.getRenderWidth() / 2,
            ctx.renderer.engine.getRenderHeight() / 2,
        )?.pickedPoint?.let { newTarget ->
            ctx.renderer.camera.target = newTarget
        }
    }

    private fun pickEntity(): Pick? {
        // Find the nearest object and NPC under the pointer.
        val pickInfo = ctx.scene.pick(ctx.scene.pointerX, ctx.scene.pointerY)
        if (pickInfo?.pickedMesh == null) return null

        val entity = (pickInfo.pickedMesh.metadata as? EntityMetadata)?.entity
            ?: return null

        // Vector from the point where we grab the entity to its position.
        val grabOffset = pickInfo.pickedMesh.position - pickInfo.pickedPoint!!

        // Vector from the point where we grab the entity to the point on the ground right beneath
        // its position. The same as grabOffset when an entity is standing on the ground.
        val dragAdjust = grabOffset.clone()

        // Find vertical distance to the ground.
        ctx.scene.pickWithRay(
            Ray(pickInfo.pickedMesh.position, DOWN_VECTOR),
            { it.isEnabled() && it.metadata is CollisionMetadata },
        )?.let { groundPick ->
            dragAdjust.y -= groundPick.distance
        }

        return Pick(
            entity,
            pickInfo.pickedMesh,
            grabOffset,
            dragAdjust,
        )
    }
}

private class TranslationState(
    private val ctx: StateContext,
    private val entity: QuestEntityModel<*, *>,
    private val dragAdjust: Vector3,
    private val grabOffset: Vector3,
) : State() {
    private val initialSection: SectionModel? = entity.section.value
    private val initialPosition: Vector3 = entity.worldPosition.value
    private var cancelled = false

    init {
        ctx.renderer.disableCameraControls()
    }

    override fun processEvent(event: Evt): State =
        when (event) {
            is PointerMoveEvt -> {
                if (cancelled) {
                    IdleState(ctx, entityManipulationEnabled = true)
                } else {
                    if (event.movedSinceLastPointerDown) {
                        ctx.translate(
                            entity,
                            dragAdjust,
                            grabOffset,
                            vertically = event.shiftKeyDown,
                        )
                    }

                    this
                }
            }

            is PointerUpEvt -> {
                ctx.renderer.enableCameraControls()

                if (!cancelled && event.movedSinceLastPointerDown) {
                    ctx.finalizeTranslation(
                        entity,
                        entity.section.value,
                        initialSection,
                        entity.worldPosition.value,
                        initialPosition,
                        true,
                    )
                }

                IdleState(ctx, entityManipulationEnabled = true)
            }

            else -> {
                if (cancelled) {
                    IdleState(ctx, entityManipulationEnabled = true)
                } else this
            }
        }

    override fun cancel() {
        cancelled = true
        ctx.renderer.enableCameraControls()

        initialSection?.let {
            entity.setSection(initialSection)
        }

        entity.setWorldPosition(initialPosition)
    }
}
