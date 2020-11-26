package world.phantasmal.web.questEditor.rendering.input

import world.phantasmal.web.externals.three.Vector2

sealed class Evt

sealed class PointerEvt : Evt() {
    abstract val buttons: Int
    abstract val shiftKeyDown: Boolean
    abstract val movedSinceLastPointerDown: Boolean

    /**
     * Pointer position in normalized device space.
     */
    abstract val pointerDevicePosition: Vector2
}

class PointerDownEvt(
    override val buttons: Int,
    override val shiftKeyDown: Boolean,
    override val movedSinceLastPointerDown: Boolean,
    override val pointerDevicePosition: Vector2,
) : PointerEvt()

class PointerUpEvt(
    override val buttons: Int,
    override val shiftKeyDown: Boolean,
    override val movedSinceLastPointerDown: Boolean,
    override val pointerDevicePosition: Vector2,
) : PointerEvt()

class PointerMoveEvt(
    override val buttons: Int,
    override val shiftKeyDown: Boolean,
    override val movedSinceLastPointerDown: Boolean,
    override val pointerDevicePosition: Vector2,
) : PointerEvt()
