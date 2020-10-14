package world.phantasmal.observable.value.list

import world.phantasmal.core.disposable.Scope
import world.phantasmal.observable.value.Val

interface ListVal<E> : Val<List<E>>, List<E> {
    val sizeVal: Val<Int>

    fun observeList(scope: Scope, observer: ListValObserver<E>)

    fun sumBy(selector: (E) -> Int): Val<Int> =
        fold(0) { acc, el -> acc + selector(el) }

    fun <R> fold(initialValue: R, operation: (R, E) -> R): Val<R> =
        FoldedVal(this, initialValue, operation)
}
