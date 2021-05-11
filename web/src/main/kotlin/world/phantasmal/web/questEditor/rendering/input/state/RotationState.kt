package world.phantasmal.web.questEditor.rendering.input.state

import world.phantasmal.web.core.minus
import world.phantasmal.web.externals.three.Vector2
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.rendering.input.Evt
import world.phantasmal.web.questEditor.rendering.input.PointerMoveEvt
import world.phantasmal.web.questEditor.rendering.input.PointerUpEvt

class RotationState(
    private val ctx: StateContext,
    private val entity: QuestEntityModel<*, *>,
    grabOffset: Vector3,
) : State() {
    private val initialRotation = entity.worldRotation.value
    private val grabPoint = entity.worldPosition.value - grabOffset
    private val pointerDevicePosition = Vector2()
    private var shouldRotate = false

    init {
        ctx.cameraInputManager.enabled = false
    }

    override fun processEvent(event: Evt): State =
        when (event) {
            is PointerMoveEvt -> {
                if (event.movedSinceLastPointerDown) {
                    pointerDevicePosition.copy(event.pointerDevicePosition)
                    shouldRotate = true
                }

                this
            }

            is PointerUpEvt -> {
                ctx.cameraInputManager.enabled = true

                if (event.movedSinceLastPointerDown) {
                    ctx.finalizeEntityRotation(
                        entity,
                        entity.worldRotation.value,
                        initialRotation,
                    )
                }

                IdleState(ctx, entityManipulationEnabled = true)
            }

            else -> this
        }

    override fun beforeRender() {
        if (shouldRotate) {
            ctx.rotateEntity(
                entity,
                initialRotation,
                grabPoint,
                pointerDevicePosition,
            )
            shouldRotate = false
        }
    }

    override fun cancel() {
        ctx.cameraInputManager.enabled = true

        entity.setWorldRotation(initialRotation)
    }
}
