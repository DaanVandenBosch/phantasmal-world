package world.phantasmal.observable.cell.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.observable.cell.Cell

interface ListCell<out E> : Cell<List<E>> {
    /**
     * Do not keep long-lived references to a [ListCell]'s [value], it may or may not be mutated
     * when the [ListCell] is mutated.
     */
    override val value: List<E>

    val size: Cell<Int>

    val empty: Cell<Boolean>

    val notEmpty: Cell<Boolean>

    operator fun get(index: Int): E

    fun observeList(callNow: Boolean = false, observer: ListObserver<E>): Disposable

    fun <R> fold(initialValue: R, operation: (R, E) -> R): Cell<R> =
        FoldedCell(this, initialValue, operation)

    fun all(predicate: (E) -> Boolean): Cell<Boolean> =
        fold(true) { acc, el -> acc && predicate(el) }

    fun sumBy(selector: (E) -> Int): Cell<Int> =
        fold(0) { acc, el -> acc + selector(el) }

    fun filtered(predicate: (E) -> Boolean): ListCell<E> =
        FilteredListCell(this, predicate)

    fun firstOrNull(): Cell<E?>

    operator fun contains(element: @UnsafeVariance E): Boolean = element in value
}
