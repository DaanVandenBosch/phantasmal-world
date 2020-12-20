package world.phantasmal.web.core.undo

import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.observable.value.nullVal
import world.phantasmal.web.core.actions.Action

class UndoManager {
    private val undos = mutableListOf<Undo>()
    private val _current = mutableVal<Undo>(NopUndo)

    val current: Val<Undo> = _current

    val canUndo: Val<Boolean> = current.flatMap { it.canUndo }
    val canRedo: Val<Boolean> = current.flatMap { it.canRedo }
    val firstUndo: Val<Action?> = current.flatMap { it.firstUndo }
    val firstRedo: Val<Action?> = current.flatMap { it.firstRedo }

    fun addUndo(undo: Undo) {
        undos.add(undo)
    }

    fun setCurrent(undo: Undo) {
        require(undo in undos) { "Undo $undo is not managed by this UndoManager." }

        _current.value = undo
    }

    fun undo(): Boolean =
        current.value.undo()

    fun redo(): Boolean =
        current.value.redo()

    /**
     * Resets all managed undos.
     */
    fun reset() {
        undos.forEach { it.reset() }
    }

    fun anyCanUndo(): Boolean =
        undos.any { it.canUndo.value }

    private object NopUndo : Undo {
        override val canUndo = falseVal()
        override val canRedo = falseVal()
        override val firstUndo = nullVal()
        override val firstRedo = nullVal()

        override fun undo(): Boolean = false

        override fun redo(): Boolean = false

        override fun reset() {
            // Do nothing.
        }
    }
}
