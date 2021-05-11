package world.phantasmal.observable.cell.list

sealed class ListChangeEvent<out E> {
    abstract val index: Int

    /**
     * Represents a structural change to the list. E.g. an element is inserted or removed.
     */
    class Change<E>(
        override val index: Int,
        /**
         * The elements that were removed from the list at [index].
         *
         * Do not keep long-lived references to a [Change]'s [removed] list, it may or may not be
         * mutated when the originating [ListCell] is mutated.
         */
        val removed: List<E>,
        /**
         * The elements that were inserted into the list at [index].
         *
         * Do not keep long-lived references to a [Change]'s [inserted] list, it may or may not be
         * mutated when the originating [ListCell] is mutated.
         */
        val inserted: List<E>,
    ) : ListChangeEvent<E>()

    /**
     * Represents a change to an element in the list. Will only be emitted if the list is configured
     * to do so.
     */
    class ElementChange<E>(
        override val index: Int,
        val updated: E,
    ) : ListChangeEvent<E>()
}

typealias ListObserver<E> = (change: ListChangeEvent<E>) -> Unit
