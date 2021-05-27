package world.phantasmal.observable.cell.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.DependentCell

interface ListCell<out E> : Cell<List<E>> {
    /**
     * Do not keep long-lived references to a [ListCell]'s [value], it may or may not be mutated
     * when the [ListCell] is mutated.
     */
    override val value: List<E>

    val size: Cell<Int>

    val empty: Cell<Boolean>

    val notEmpty: Cell<Boolean>

    operator fun get(index: Int): E = value[index]

    fun observeList(callNow: Boolean = false, observer: ListObserver<E>): Disposable

    fun <R> fold(initialValue: R, operation: (R, E) -> R): Cell<R> =
        DependentCell(this) { value.fold(initialValue, operation) }

    fun all(predicate: (E) -> Boolean): Cell<Boolean> =
        DependentCell(this) { value.all(predicate) }

    fun sumOf(selector: (E) -> Int): Cell<Int> =
        DependentCell(this) { value.sumOf(selector) }

    fun filtered(predicate: (E) -> Boolean): ListCell<E> =
        FilteredListCell(this, predicate)

    fun firstOrNull(): Cell<E?> =
        DependentCell(this) { value.firstOrNull() }

    operator fun contains(element: @UnsafeVariance E): Boolean = element in value
}
