package world.phantasmal.web.core.undo

import world.phantasmal.cell.Cell
import world.phantasmal.cell.and
import world.phantasmal.cell.falseCell
import world.phantasmal.cell.flatMap
import world.phantasmal.cell.flatten
import world.phantasmal.cell.list.fold
import world.phantasmal.cell.list.mutableListCell
import world.phantasmal.cell.mutableCell
import world.phantasmal.cell.nullCell
import world.phantasmal.cell.trueCell
import world.phantasmal.web.core.commands.Command

class UndoManager {
    private val undos = mutableListCell<Undo>(NopUndo)
    private val _current = mutableCell<Undo>(NopUndo)

    val current: Cell<Undo> = _current

    val canUndo: Cell<Boolean> = current.flatMap { it.canUndo }
    val canRedo: Cell<Boolean> = current.flatMap { it.canRedo }
    val firstUndo: Cell<Command?> = current.flatMap { it.firstUndo }
    val firstRedo: Cell<Command?> = current.flatMap { it.firstRedo }

    /**
     * True if all undos are at the most recent save point. I.e., true if there are no changes to
     * save.
     */
    // TODO: Optimize this once ListCell supports more performant method for this use-case.
    val allAtSavePoint: Cell<Boolean> =
        undos.fold(trueCell()) { acc, undo -> acc and undo.atSavePoint }.flatten()

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
        override val canUndo = falseCell()
        override val canRedo = falseCell()
        override val firstUndo = nullCell()
        override val firstRedo = nullCell()
        override val atSavePoint = trueCell()

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
