package world.phantasmal.web.questEditor.rendering.input.state

import world.phantasmal.web.externals.three.Vector2
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.SectionModel
import world.phantasmal.web.questEditor.rendering.input.Evt
import world.phantasmal.web.questEditor.rendering.input.PointerMoveEvt
import world.phantasmal.web.questEditor.rendering.input.PointerUpEvt

class TranslationState(
    private val ctx: StateContext,
    private val entity: QuestEntityModel<*, *>,
    private val dragAdjust: Vector3,
    private val grabOffset: Vector3,
) : State() {
    private val initialSection: SectionModel? = entity.section.value
    private val initialPosition: Vector3 = entity.position.value
    private val pointerDevicePosition = Vector2()
    private var shouldTranslate = false
    private var shouldTranslateVertically = false
    private var shouldAdjustSection = false

    init {
        ctx.cameraInputManager.enabled = false
    }

    override fun processEvent(event: Evt): State =
        when (event) {
            is PointerMoveEvt -> {
                if (event.movedSinceLastPointerDown) {
                    pointerDevicePosition.copy(event.pointerDevicePosition)
                    shouldTranslate = true
                    shouldTranslateVertically = event.shiftKey
                    shouldAdjustSection = !event.ctrlKey
                }

                this
            }

            is PointerUpEvt -> {
                ctx.cameraInputManager.enabled = true

                if (event.movedSinceLastPointerDown) {
                    ctx.finalizeEntityTranslation(
                        entity,
                        entity.section.value,
                        initialSection,
                        entity.position.value,
                        initialPosition,
                    )
                }

                IdleState(ctx, entityManipulationEnabled = true)
            }

            else -> this
        }

    override fun beforeRender() {
        if (shouldTranslate) {
            if (shouldTranslateVertically) {
                ctx.translateEntityVertically(
                    entity,
                    dragAdjust,
                    grabOffset,
                    pointerDevicePosition,
                )
            } else {
                ctx.translateEntityHorizontally(
                    entity,
                    dragAdjust,
                    grabOffset,
                    pointerDevicePosition,
                    shouldAdjustSection,
                )
            }

            shouldTranslate = false
        }
    }

    override fun cancel() {
        ctx.cameraInputManager.enabled = true

        initialSection?.let {
            entity.setSection(initialSection)
        }

        entity.setPosition(initialPosition)
    }
}
