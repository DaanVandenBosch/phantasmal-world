package world.phantasmal.observable.value.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.observable.value.Val

interface ListVal<out E> : Val<List<E>> {
    /**
     * Do not keep long-lived references to a [ListVal]'s [value], it may or may not be mutated
     * when the [ListVal] is mutated.
     */
    override val value: List<E>

    val size: Val<Int>

    val empty: Val<Boolean>

    val notEmpty: Val<Boolean>

    operator fun get(index: Int): E

    fun observeList(callNow: Boolean = false, observer: ListValObserver<E>): Disposable

    fun <R> fold(initialValue: R, operation: (R, E) -> R): Val<R> =
        FoldedVal(this, initialValue, operation)

    fun all(predicate: (E) -> Boolean): Val<Boolean> =
        fold(true) { acc, el -> acc && predicate(el) }

    fun sumBy(selector: (E) -> Int): Val<Int> =
        fold(0) { acc, el -> acc + selector(el) }

    fun filtered(predicate: (E) -> Boolean): ListVal<E> =
        FilteredListVal(this, predicate)

    fun firstOrNull(): Val<E?>

    operator fun contains(element: @UnsafeVariance E): Boolean = element in value
}
