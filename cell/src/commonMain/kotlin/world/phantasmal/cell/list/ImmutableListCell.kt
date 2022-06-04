package world.phantasmal.cell.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.nopDisposable
import world.phantasmal.cell.ChangeObserver
import world.phantasmal.cell.Dependency
import world.phantasmal.cell.Dependent
import world.phantasmal.cell.Cell
import world.phantasmal.cell.cell
import world.phantasmal.cell.falseCell
import world.phantasmal.cell.trueCell

class ImmutableListCell<E>(private val elements: List<E>) : Dependency<List<E>>, ListCell<E> {
    override val size: Cell<Int> = cell(elements.size)
    override val empty: Cell<Boolean> = if (elements.isEmpty()) trueCell() else falseCell()
    override val notEmpty: Cell<Boolean> = if (elements.isNotEmpty()) trueCell() else falseCell()

    override val value: List<E> = elements

    override val changeEvent: ListChangeEvent<E>? get() = null

    override fun addDependent(dependent: Dependent) {
        // We don't remember our dependents because we never need to notify them of changes.
    }

    override fun removeDependent(dependent: Dependent) {
        // Nothing to remove because we don't remember our dependents.
    }

    override fun get(index: Int): E = elements[index]

    override fun observeChange(observer: ChangeObserver<List<E>>): Disposable = nopDisposable()

    override fun observeListChange(observer: ListChangeObserver<E>): Disposable = nopDisposable()

    override fun toString(): String = listCellToString(this)
}
