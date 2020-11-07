package world.phantasmal.web.questEditor.rendering

import kotlinx.browser.document
import mu.KotlinLogging
import org.w3c.dom.events.Event
import org.w3c.dom.pointerevents.PointerEvent
import world.phantasmal.web.core.minusAssign
import world.phantasmal.web.externals.babylon.AbstractMesh
import world.phantasmal.web.externals.babylon.Vector2
import world.phantasmal.web.externals.babylon.Vector3
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.rendering.conversion.EntityMetadata
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.dom.disposableListener

private val logger = KotlinLogging.logger {}

class EntityManipulator(
    private val questEditorStore: QuestEditorStore,
    private val renderer: QuestRenderer,
) : DisposableContainer() {
    private val pointerPosition = Vector2.Zero()
    private val lastPointerPosition = Vector2.Zero()
    private var movedSinceLastPointerDown = false
    private var state: State

    /**
     * Whether entity transformations, deletions, etc. are enabled or not.
     * Hover over and selection still work when this is set to false.
     */
    var enabled: Boolean = true
        set(enabled) {
            field = enabled
            state.cancel()
            state = IdleState(questEditorStore, renderer, enabled)
        }

    init {
        state = IdleState(questEditorStore, renderer, enabled)

        observe(questEditorStore.selectedEntity, ::selectedEntityChanged)

        addDisposables(
            disposableListener(renderer.canvas, "pointerdown", ::onPointerDown)
        )
    }

    private fun selectedEntityChanged(entity: QuestEntityModel<*, *>?) {
        state.cancel()
    }

    private fun onPointerDown(e: PointerEvent) {
        processPointerEvent(e)

        state = state.processEvent(PointerDownEvt(
            e.buttons.toInt(),
            movedSinceLastPointerDown
        ))

        document.addEventListener("pointerup", ::onPointerUp)
    }

    private fun onPointerUp(e: Event) {
        try {
            e as PointerEvent
            processPointerEvent(e)

            state = state.processEvent(PointerUpEvt(
                e.buttons.toInt(),
                movedSinceLastPointerDown
            ))
        } finally {
            document.removeEventListener("pointerup", ::onPointerUp)
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
     * Vector that points from the grabbing point (somewhere on the model's surface) to the model's
     * origin.
     */
    val grabOffset: Vector3,

    /**
     * Vector that points from the grabbing point to the terrain point directly under the model's
     * origin.
     */
//    val dragAdjust: Vector3,
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
    private val enabled: Boolean,
) : State() {
    override fun processEvent(event: Evt): State =
        when (event) {
            is PointerDownEvt -> {
                pickEntity()?.let { pick ->
                    when (event.buttons) {
                        1 -> {
                            questEditorStore.setSelectedEntity(pick.entity)

                            if (enabled) {
                                // TODO: Enter TranslationState.
                            }
                        }
                        2 -> {
                            questEditorStore.setSelectedEntity(pick.entity)

                            if (enabled) {
                                // TODO: Enter RotationState.
                            }
                        }
                    }
                }

                this
            }

            is PointerUpEvt -> {
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

    private fun pickEntity(): Pick? {
        // Find the nearest object and NPC under the pointer.
        val pickInfo = renderer.scene.pick(renderer.scene.pointerX, renderer.scene.pointerY)
        if (pickInfo?.pickedMesh == null) return null

        val entity = (pickInfo.pickedMesh.metadata as? EntityMetadata)?.entity
            ?: return null
        val grabOffset = pickInfo.pickedMesh.position.clone()
        grabOffset -= pickInfo.pickedPoint!!

        // TODO: dragAdjust.
//        val dragAdjust = grabOffset.clone()
//
//        // Find vertical distance to the ground.
//        raycaster.set(intersection.object.position, DOWN_VECTOR)
//        val [collision_geom_intersection] = raycaster.intersectObjects(
//        this.renderer.collision_geometry.children,
//        true,
//        )
//
//        if (collision_geom_intersection) {
//            dragAdjust.y -= collision_geom_intersection.distance
//        }

        return Pick(
            entity,
            pickInfo.pickedMesh,
            grabOffset,
        )
    }
}
