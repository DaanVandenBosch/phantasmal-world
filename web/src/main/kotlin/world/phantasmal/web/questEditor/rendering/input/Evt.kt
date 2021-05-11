package world.phantasmal.web.questEditor.rendering.input

import world.phantasmal.lib.fileFormats.quest.EntityType
import world.phantasmal.web.externals.three.Vector2
import world.phantasmal.web.questEditor.widgets.EntityDragEvent

sealed class Evt

class KeyboardEvt(val key: String) : Evt()

sealed class PointerEvt : Evt() {
    abstract val buttons: Int
    abstract val ctrlKey: Boolean
    abstract val shiftKey: Boolean

    /**
     * Pointer position in normalized device space.
     */
    abstract val pointerDevicePosition: Vector2
    abstract val movedSinceLastPointerDown: Boolean
}

class PointerDownEvt(
    override val buttons: Int,
    override val ctrlKey: Boolean,
    override val shiftKey: Boolean,
    override val pointerDevicePosition: Vector2,
    override val movedSinceLastPointerDown: Boolean,
) : PointerEvt()

class PointerUpEvt(
    override val buttons: Int,
    override val ctrlKey: Boolean,
    override val shiftKey: Boolean,
    override val pointerDevicePosition: Vector2,
    override val movedSinceLastPointerDown: Boolean,
) : PointerEvt()

class PointerMoveEvt(
    override val buttons: Int,
    override val ctrlKey: Boolean,
    override val shiftKey: Boolean,
    override val pointerDevicePosition: Vector2,
    override val movedSinceLastPointerDown: Boolean,
) : PointerEvt()

class PointerOutEvt(
    override val buttons: Int,
    override val ctrlKey: Boolean,
    override val shiftKey: Boolean,
    override val pointerDevicePosition: Vector2,
    override val movedSinceLastPointerDown: Boolean,
) : PointerEvt()

sealed class EntityDragEvt(
    private val event: EntityDragEvent,
    /**
     * Pointer position in normalized device space.
     */
    val pointerDevicePosition: Vector2,
) : Evt() {
    val entityType: EntityType = event.entityType
    val ctrlKey: Boolean = event.ctrlKey
    val shiftKey: Boolean = event.shiftKey

    fun allowDrop() {
        event.allowDrop()
    }

    fun showDragElement() {
        event.showDragElement()
    }

    fun hideDragElement() {
        event.hideDragElement()
    }
}

class EntityDragEnterEvt(
    event: EntityDragEvent,
    pointerDevicePosition: Vector2,
) : EntityDragEvt(event, pointerDevicePosition)

class EntityDragOverEvt(
    event: EntityDragEvent,
    pointerDevicePosition: Vector2,
) : EntityDragEvt(event, pointerDevicePosition)

class EntityDragLeaveEvt(
    event: EntityDragEvent,
    pointerDevicePosition: Vector2,
) : EntityDragEvt(event, pointerDevicePosition)

class EntityDropEvt(
    event: EntityDragEvent,
    pointerDevicePosition: Vector2,
) : EntityDragEvt(event, pointerDevicePosition)
