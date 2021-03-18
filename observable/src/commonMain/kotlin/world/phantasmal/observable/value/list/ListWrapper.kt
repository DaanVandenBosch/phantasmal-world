package world.phantasmal.observable.value.list

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Wrapper is used to ensure that ListVal.value of some implementations references a new object
 * after every change to the ListVal. This is done to honor the contract that emission of a
 * ChangeEvent implies that Val.value is no longer equal to the previous value.
 * When a change is made to the ListVal, the underlying list of Wrapper is usually mutated and then
 * a new Wrapper is created that points to the same underlying list.
 */
internal class ListWrapper<E>(private val mut: MutableList<E>) : List<E> by mut {
    inline fun mutate(mutator: MutableList<E>.() -> Unit): ListWrapper<E> {
        contract { callsInPlace(mutator, InvocationKind.EXACTLY_ONCE) }
        mut.mutator()
        return ListWrapper(mut)
    }
}
