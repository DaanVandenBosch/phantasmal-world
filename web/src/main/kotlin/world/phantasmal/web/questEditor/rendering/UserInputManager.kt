package world.phantasmal.web.questEditor.rendering

import kotlinx.browser.document
import mu.KotlinLogging
import org.w3c.dom.pointerevents.PointerEvent
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.web.core.minus
import world.phantasmal.web.core.plusAssign
import world.phantasmal.web.externals.three.*
import world.phantasmal.web.questEditor.actions.TranslateEntityAction
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.SectionModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.dom.disposableListener

private val logger = KotlinLogging.logger {}

private val ZERO_VECTOR_2 = Vector2(0.0, 0.0)
private val ZERO_VECTOR_3 = Vector3(0.0, 0.0, 0.0)
private val UP_VECTOR = Vector3(0.0, 1.0, 0.0)
private val DOWN_VECTOR = Vector3(0.0, -1.0, 0.0)

class UserInputManager(
    questEditorStore: QuestEditorStore,
    private val renderer: QuestRenderer,
) : DisposableContainer() {
    private val stateContext = StateContext(questEditorStore, renderer)
    private val pointerPosition = Vector2()
    private val pointerDevicePosition = Vector2()
    private val lastPointerPosition = Vector2()
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

        renderer.initializeControls()
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
                pointerDevicePosition,
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
                    pointerDevicePosition,
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
                pointerDevicePosition,
            )
        )
    }

    private fun processPointerEvent(e: PointerEvent) {
        val rect = renderer.canvas.getBoundingClientRect()
        pointerPosition.set(e.clientX - rect.left, e.clientY - rect.top)
        pointerDevicePosition.copy(pointerPosition)
        renderer.pointerPosToDeviceCoords(pointerDevicePosition)

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

        lastPointerPosition.copy(pointerPosition)
    }
}

private class StateContext(
    private val questEditorStore: QuestEditorStore,
    val renderer: QuestRenderer,
) {
    val scene = renderer.scene

    fun setHighlightedEntity(entity: QuestEntityModel<*, *>?) {
        questEditorStore.setHighlightedEntity(entity)
    }

    fun setSelectedEntity(entity: QuestEntityModel<*, *>?) {
        questEditorStore.setSelectedEntity(entity)
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
     * @param origin position in normalized device space.
     */
    fun pickGround(origin: Vector2, dragAdjust: Vector3 = ZERO_VECTOR_3): Intersection? =
        intersectObject(origin, renderer.collisionGeometry, dragAdjust) { intersection ->
            // Don't allow entities to be placed on very steep terrain. E.g. walls.
            // TODO: make use of the flags field in the collision data.
            intersection.face?.normal?.let { n -> n.y > 0.75 } ?: false
        }

    inline fun intersectObject(
        origin: Vector3,
        direction: Vector3,
        obj3d: Object3D,
        predicate: (Intersection) -> Boolean = { true },
    ): Intersection? {
        raycaster.set(origin, direction)
        raycasterIntersections.asDynamic().splice(0)
        raycaster.intersectObject(obj3d, recursive = true, raycasterIntersections)
        return raycasterIntersections.find(predicate)
    }

    /**
     * The ray's direction is determined by the camera.
     *
     * @param origin ray origin in normalized device space.
     * @param translateOrigin vector by which to translate the ray's origin after construction from
     * the camera.
     */
    inline fun intersectObject(
        origin: Vector2,
        obj3d: Object3D,
        translateOrigin: Vector3 = ZERO_VECTOR_3,
        predicate: (Intersection) -> Boolean = { true },
    ): Intersection? {
        raycaster.setFromCamera(origin, renderer.camera)
        raycaster.ray.origin += translateOrigin
        raycasterIntersections.asDynamic().splice(0)
        raycaster.intersectObject(obj3d, recursive = true, raycasterIntersections)
        return raycasterIntersections.find(predicate)
    }

    fun intersectPlane(origin: Vector2, plane: Plane, intersectionPoint: Vector3): Vector3? {
        raycaster.setFromCamera(origin, renderer.camera)
        return raycaster.ray.intersectPlane(plane, intersectionPoint)
    }

    companion object {
        private val raycaster = Raycaster()
        private val raycasterIntersections = arrayOf<Intersection>()
    }
}

private sealed class Evt

private sealed class PointerEvt : Evt() {
    abstract val buttons: Int
    abstract val shiftKeyDown: Boolean
    abstract val movedSinceLastPointerDown: Boolean

    /**
     * Pointer position in normalized device space.
     */
    abstract val pointerDevicePosition: Vector2
}

private class PointerDownEvt(
    override val buttons: Int,
    override val shiftKeyDown: Boolean,
    override val movedSinceLastPointerDown: Boolean,
    override val pointerDevicePosition: Vector2,
) : PointerEvt()

private class PointerUpEvt(
    override val buttons: Int,
    override val shiftKeyDown: Boolean,
    override val movedSinceLastPointerDown: Boolean,
    override val pointerDevicePosition: Vector2,
) : PointerEvt()

private class PointerMoveEvt(
    override val buttons: Int,
    override val shiftKeyDown: Boolean,
    override val movedSinceLastPointerDown: Boolean,
    override val pointerDevicePosition: Vector2,
) : PointerEvt()

private class Pick(
    val entity: QuestEntityModel<*, *>,
    val mesh: InstancedMesh,

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
    private var panning = false
    private var rotating = false
    private var zooming = false

    override fun processEvent(event: Evt): State {
        when (event) {
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
                                    pick.grabOffset
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
                                // TODO: Enter RotationState.
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
                }
            }

            is PointerMoveEvt -> {
                if (!panning && !rotating && !zooming) {
                    // User is hovering.
                    ctx.setHighlightedEntity(pickEntity(event.pointerDevicePosition)?.entity)
                }
            }
        }

        return this
    }

    override fun cancel() {
        // Do nothing.
    }

    private fun updateCameraTarget() {
        // If the user moved the camera, try setting the camera target to a better point.
        ctx.pickGround(ZERO_VECTOR_2)?.let { intersection ->
            ctx.renderer.controls.target = intersection.point
            ctx.renderer.controls.update()
        }
    }

    /**
     * @param pointerPosition pointer coordinates in normalized device space
     */
    private fun pickEntity(pointerPosition: Vector2): Pick? {
        // Find the nearest entity under the pointer.
        val intersection = ctx.intersectObject(
            pointerPosition,
            ctx.renderer.entities,
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
            ctx.renderer.collisionGeometry,
        )?.let { groundIntersection ->
            dragAdjust.y -= groundIntersection.distance
        }

        return Pick(
            entity,
            intersection.`object` as InstancedMesh,
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
        ctx.renderer.controls.enabled = false
    }

    override fun processEvent(event: Evt): State =
        when (event) {
            is PointerMoveEvt -> {
                if (cancelled) {
                    IdleState(ctx, entityManipulationEnabled = true)
                } else {
                    if (event.movedSinceLastPointerDown) {
                        translate(event.pointerDevicePosition, vertically = event.shiftKeyDown)
                    }

                    this
                }
            }

            is PointerUpEvt -> {
                ctx.renderer.controls.enabled = true

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
        ctx.renderer.controls.enabled = true

        initialSection?.let {
            entity.setSection(initialSection)
        }

        entity.setWorldPosition(initialPosition)
    }

    /**
     * @param pointerPosition pointer position in normalized device space
     */
    private fun translate(pointerPosition: Vector2, vertically: Boolean) {
        if (vertically) {
            // TODO: Vertical translation.
        } else {
            translateEntityHorizontally(pointerPosition)
        }
    }

    /**
     * If the drag-adjusted pointer is over the ground, translate an entity horizontally across the
     * ground. Otherwise translate the entity over the horizontal plane that intersects its origin.
     */
    private fun translateEntityHorizontally(pointerPosition: Vector2) {
        val pick = ctx.pickGround(pointerPosition, dragAdjust)

        if (pick == null) {
            // If the pointer is not over the ground, we translate the entity across the horizontal
            // plane in which the entity's origin lies.
            plane.set(UP_VECTOR, -entity.worldPosition.value.y + grabOffset.y)

            ctx.intersectPlane(pointerPosition, plane, tmpVec)?.let { pointerPosOnPlane ->
                entity.setWorldPosition(Vector3(
                    pointerPosOnPlane.x + grabOffset.x,
                    entity.worldPosition.value.y,
                    pointerPosOnPlane.z + grabOffset.z,
                ))
            }
        } else {
            // TODO: Set entity section.
            entity.setWorldPosition(
                Vector3(
                    pick.point.x,
                    pick.point.y + grabOffset.y - dragAdjust.y,
                    pick.point.z,
                )
            )
        }
    }

    companion object {
        private val plane = Plane()
        private val tmpVec = Vector3()
    }
}
