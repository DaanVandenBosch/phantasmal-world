package world.phantasmal.web.core.undo

import world.phantasmal.observable.value.*
import world.phantasmal.observable.value.list.mutableListVal
import world.phantasmal.web.core.actions.Action

class UndoManager {
    private val undos = mutableListVal<Undo>(NopUndo) { arrayOf(it.atSavePoint) }
    private val _current = mutableVal<Undo>(NopUndo)

    val current: Val<Undo> = _current

    val canUndo: Val<Boolean> = current.flatMap { it.canUndo }
    val canRedo: Val<Boolean> = current.flatMap { it.canRedo }
    val firstUndo: Val<Action?> = current.flatMap { it.firstUndo }
    val firstRedo: Val<Action?> = current.flatMap { it.firstRedo }

    /**
     * True if all undos are at the most recent save point. I.e., true if there are no changes to
     * save.
     */
    val allAtSavePoint: Val<Boolean> = undos.all { it.atSavePoint.value }

    fun addUndo(undo: Undo) {
        undos.add(undo)
    }

    fun makeNopCurrent() {
        setCurrent(NopUndo)
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
     * Sets a save point on all undos.
     */
    fun savePoint() {
        undos.value.forEach { it.savePoint() }
    }

    /**
     * Resets all managed undos.
     */
    fun reset() {
        undos.value.forEach { it.reset() }
    }

    private object NopUndo : Undo {
        override val canUndo = falseVal()
        override val canRedo = falseVal()
        override val firstUndo = nullVal()
        override val firstRedo = nullVal()
        override val atSavePoint = trueVal()

        override fun undo(): Boolean = false

        override fun redo(): Boolean = false

        override fun savePoint() {
            // Do nothing.
        }

        override fun reset() {
            // Do nothing.
        }
    }
}
