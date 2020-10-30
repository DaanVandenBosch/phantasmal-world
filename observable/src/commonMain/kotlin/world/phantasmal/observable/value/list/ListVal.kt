package world.phantasmal.observable.value.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.observable.value.Val

interface ListVal<E> : Val<List<E>> {
    val sizeVal: Val<Int>

    fun observeList(observer: ListValObserver<E>): Disposable

    fun sumBy(selector: (E) -> Int): Val<Int> =
        fold(0) { acc, el -> acc + selector(el) }

    fun <R> fold(initialValue: R, operation: (R, E) -> R): Val<R> =
        FoldedVal(this, initialValue, operation)
}
