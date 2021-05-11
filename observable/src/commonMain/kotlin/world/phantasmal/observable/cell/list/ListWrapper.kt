package world.phantasmal.observable.cell.list

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * ListWrapper is used to ensure that ListCell.value of some implementations references a new object
 * after every change to the ListCell. This is done to honor the contract that emission of a
 * ChangeEvent implies that Cell.value is no longer equal to the previous value.
 * When a change is made to the ListCell, the underlying list of ListWrapper is usually mutated and
 * then a new wrapper is created that points to the same underlying list.
 */
internal class ListWrapper<E>(private val mut: MutableList<E>) : List<E> by mut {
    inline fun mutate(mutator: MutableList<E>.() -> Unit): ListWrapper<E> {
        contract { callsInPlace(mutator, InvocationKind.EXACTLY_ONCE) }
        mut.mutator()
        return ListWrapper(mut)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        // If other is also a ListWrapper but it's not the exact same object then it's not equal.
        if (other == null || this::class == other::class || other !is List<*>) return false
        // If other is a list but not a ListWrapper, call its equals method for a structured
        // comparison.
        return other == this
    }

    override fun hashCode(): Int = mut.hashCode()
}
