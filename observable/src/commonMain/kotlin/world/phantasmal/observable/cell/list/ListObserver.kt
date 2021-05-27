package world.phantasmal.observable.cell.list

import world.phantasmal.observable.ChangeEvent

class ListChangeEvent<out E>(
    value: List<E>,
    val changes: List<ListChange<E>>,
) : ChangeEvent<List<E>>(value)

sealed class ListChange<out E> {
    /**
     * Represents a structural change to a list cell. E.g. an element is inserted or removed.
     */
    class Structural<out E>(
        val index: Int,
        /**
         * The elements that were removed from the list at [index].
         *
         * Do not keep long-lived references to a [ChangeEvent]'s [removed] list, it may or may not
         * be mutated when the originating [ListCell] is mutated.
         */
        val removed: List<E>,
        /**
         * The elements that were inserted into the list at [index].
         *
         * Do not keep long-lived references to a [ChangeEvent]'s [inserted] list, it may or may not
         * be mutated when the originating [ListCell] is mutated.
         */
        val inserted: List<E>,
    ) : ListChange<E>()

    /**
     * Represents a change to an element in a list cell. Will only be emitted if the list is
     * configured to do so.
     */
    class Element<E>(
        val index: Int,
        val updated: E,
    ) : ListChange<E>()
}

typealias ListObserver<E> = (ListChangeEvent<E>) -> Unit
