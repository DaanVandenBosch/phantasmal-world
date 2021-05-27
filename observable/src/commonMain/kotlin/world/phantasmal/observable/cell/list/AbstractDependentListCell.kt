package world.phantasmal.observable.cell.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.observable.CallbackObserver
import world.phantasmal.observable.Dependent
import world.phantasmal.observable.Observer
import world.phantasmal.observable.cell.AbstractDependentCell
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.DependentCell
import world.phantasmal.observable.cell.not

abstract class AbstractDependentListCell<E> :
    AbstractDependentCell<List<E>>(),
    ListCell<E>,
    Dependent {

    protected abstract val elements: List<E>

    override val value: List<E>
        get() {
            if (dependents.isEmpty()) {
                computeElements()
            }

            return elements
        }

    @Suppress("LeakingThis")
    final override val size: Cell<Int> = DependentCell(this) { elements.size }

    final override val empty: Cell<Boolean> = size.map { it == 0 }

    final override val notEmpty: Cell<Boolean> = !empty

    final override fun observe(callNow: Boolean, observer: Observer<List<E>>): Disposable =
        observeList(callNow, observer as ListObserver<E>)

    override fun observeList(callNow: Boolean, observer: ListObserver<E>): Disposable {
        val observingCell = CallbackObserver(this, observer)

        if (callNow) {
            observer(
                ListChangeEvent(
                    value,
                    listOf(
                        ListChange.Structural(index = 0, removed = emptyList(), inserted = value),
                    ),
                )
            )
        }

        return observingCell
    }

    final override fun dependenciesChanged() {
        val oldElements = value

        computeElements()

        emitChanged(
            ListChangeEvent(elements, listOf(ListChange.Structural(0, oldElements, elements)))
        )
    }

    protected abstract fun computeElements()
}
