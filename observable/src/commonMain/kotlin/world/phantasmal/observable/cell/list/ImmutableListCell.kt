package world.phantasmal.observable.cell.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.nopDisposable
import world.phantasmal.observable.ChangeObserver
import world.phantasmal.observable.Dependency
import world.phantasmal.observable.Dependent
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.cell
import world.phantasmal.observable.cell.falseCell
import world.phantasmal.observable.cell.trueCell

class ImmutableListCell<E>(private val elements: List<E>) : Dependency, ListCell<E> {
    override val size: Cell<Int> = cell(elements.size)
    override val empty: Cell<Boolean> = if (elements.isEmpty()) trueCell() else falseCell()
    override val notEmpty: Cell<Boolean> = if (elements.isNotEmpty()) trueCell() else falseCell()

    override val value: List<E> = elements

    override fun addDependent(dependent: Dependent) {
        // We don't remember our dependents because we never need to notify them of changes.
    }

    override fun removeDependent(dependent: Dependent) {
        // Nothing to remove because we don't remember our dependents.
    }

    override fun get(index: Int): E =
        elements[index]

    override fun observeChange(observer: ChangeObserver<List<E>>): Disposable = nopDisposable()

    override fun observeListChange(observer: ListChangeObserver<E>): Disposable = nopDisposable()

    override fun emitDependencyChanged() {
        error("ImmutableListCell can't change.")
    }
}
