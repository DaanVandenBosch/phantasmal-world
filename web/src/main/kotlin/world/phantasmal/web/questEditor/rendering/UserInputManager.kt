package world.phantasmal.web.questEditor.rendering

import kotlinx.browser.document
import mu.KotlinLogging
import org.w3c.dom.pointerevents.PointerEvent
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.web.core.minus
import world.phantasmal.web.externals.babylon.*
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.dom.disposableListener

private val logger = KotlinLogging.logger {}

private val DOWN_VECTOR = Vector3.Down()

class UserInputManager(
    private val questEditorStore: QuestEditorStore,
    private val renderer: QuestRenderer,
) : DisposableContainer() {
    private val pointerPosition = Vector2.Zero()
    private val lastPointerPosition = Vector2.Zero()
    private var movedSinceLastPointerDown = false
    private var state: State
    private var onPointerUpListener: Disposable? = null

    /**
     * Whether entity transformations, deletions, etc. are enabled or not.
     * Hover over and selection still work when this is set to false.
     */
    var entityManipulationEnabled: Boolean = true
        set(enabled) {
            field = enabled
            state.cancel()
            state = IdleState(questEditorStore, renderer, enabled)
        }

    init {
        state = IdleState(questEditorStore, renderer, entityManipulationEnabled)

        observe(questEditorStore.selectedEntity) { state.cancel() }

        addDisposables(
            disposableListener(renderer.canvas, "pointerdown", ::onPointerDown)
        )
    }

    private fun onPointerDown(e: PointerEvent) {
        processPointerEvent(e)

        state = state.processEvent(PointerDownEvt(
            e.buttons.toInt(),
            movedSinceLastPointerDown
        ))

        onPointerUpListener = disposableListener(document, "pointerup", ::onPointerUp)
    }

    private fun onPointerUp(e: PointerEvent) {
        try {
            processPointerEvent(e)

            state = state.processEvent(PointerUpEvt(
                e.buttons.toInt(),
                movedSinceLastPointerDown
            ))
        } finally {
            onPointerUpListener?.dispose()
            onPointerUpListener = null
        }
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

private sealed class Evt

private sealed class PointerEvt : Evt() {
    abstract val buttons: Int
    abstract val movedSinceLastPointerDown: Boolean
}

private class PointerDownEvt(
    override val buttons: Int,
    override val movedSinceLastPointerDown: Boolean,
) : PointerEvt()

private class PointerUpEvt(
    override val buttons: Int,
    override val movedSinceLastPointerDown: Boolean,
) : PointerEvt()

private class Pick(
    val entity: QuestEntityModel<*, *>,
    val mesh: AbstractMesh,

    /**
     * Vector that points from the grabbing point (somewhere on the model's surface) to the entity's
     * position.
     */
    val grabOffset: Vector3,

    /**
     * Vector that points from the grabbing point to the terrain point directly under the entity's
     * position.
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
    private val questEditorStore: QuestEditorStore,
    private val renderer: QuestRenderer,
    private val entityManipulationEnabled: Boolean,
) : State() {
    override fun processEvent(event: Evt): State =
        when (event) {
            is PointerDownEvt -> {
                pickEntity()?.let { pick ->
                    when (event.buttons) {
                        1 -> {
                            questEditorStore.setSelectedEntity(pick.entity)

                            if (entityManipulationEnabled) {
                                // TODO: Enter TranslationState.
                            }
                        }
                        2 -> {
                            questEditorStore.setSelectedEntity(pick.entity)

                            if (entityManipulationEnabled) {
                                // TODO: Enter RotationState.
                            }
                        }
                    }
                }

                this
            }

            is PointerUpEvt -> {
                updateCameraTarget()

                // If the user clicks on nothing, deselect the currently selected entity.
                if (!event.movedSinceLastPointerDown && pickEntity() == null) {
                    questEditorStore.setSelectedEntity(null)
                }

                this
            }
        }

    override fun cancel() {
        // Do nothing.
    }

    private fun updateCameraTarget() {
        // If the user moved the camera, try setting the camera
        // target to a better point.
        pickGround()?.pickedPoint?.let { newTarget ->
            renderer.camera.target = newTarget
        }
    }

    private fun pickEntity(): Pick? {
        // Find the nearest object and NPC under the pointer.
        val pickInfo = renderer.scene.pick(renderer.scene.pointerX, renderer.scene.pointerY)
        if (pickInfo?.pickedMesh == null) return null

        val entity = (pickInfo.pickedMesh.metadata as? EntityMetadata)?.entity
            ?: return null

        // Vector from the point where we grab the entity to its position.
        val grabOffset = pickInfo.pickedMesh.position - pickInfo.pickedPoint!!

        // Vector from the point where we grab the entity to the point on the ground right beneath
        // its position. The same as grabOffset when an entity is standing on the ground.
        val dragAdjust = grabOffset.clone()

        // Find vertical distance to the ground.
        renderer.scene.pickWithRay(
            Ray(pickInfo.pickedMesh.position, DOWN_VECTOR),
            { it.metadata is CollisionMetadata },
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

    private fun pickGround(): PickingInfo? {
        renderer.scene.multiPick(
            renderer.engine.getRenderWidth() / 2,
            renderer.engine.getRenderHeight() / 2,
            { it.metadata is CollisionMetadata },
            renderer.camera,
        )?.let { pickingInfoArray ->
            // Don't allow entities to be placed on very steep terrain.
            // E.g. walls.
            // TODO: make use of the flags field in the collision data.
            for (pickingInfo in pickingInfoArray) {
                pickingInfo.getNormal()?.let { n ->
                    if (n.y > 0.75) {
                        return pickingInfo
                    }
                }
            }
        }

        return null
    }
}
