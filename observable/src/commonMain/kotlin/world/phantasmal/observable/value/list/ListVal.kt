package world.phantasmal.observable.value.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.observable.value.Val

interface ListVal<E> : Val<List<E>> {
    val size: Val<Int>

    operator fun get(index: Int): E

    fun observeList(callNow: Boolean = false, observer: ListValObserver<E>): Disposable

    fun sumBy(selector: (E) -> Int): Val<Int> =
        fold(0) { acc, el -> acc + selector(el) }

    fun <R> fold(initialValue: R, operation: (R, E) -> R): Val<R> =
        FoldedVal(this, initialValue, operation)

    fun filtered(predicate: (E) -> Boolean): ListVal<E> =
        FilteredListVal(this, predicate)

    fun firstOrNull(): Val<E?>
}
