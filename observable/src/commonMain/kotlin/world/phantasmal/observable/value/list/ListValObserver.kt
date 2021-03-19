package world.phantasmal.observable.value.list

sealed class ListValChangeEvent<out E> {
    abstract val index: Int

    class Change<E>(
        override val index: Int,
        /**
         * Do not keep long-lived references to a [Change]'s [removed] list, it may or may not be
         * mutated when the originating [ListVal] is mutated.
         */
        val removed: List<E>,
        /**
         * Do not keep long-lived references to a [Change]'s [inserted] list, it may or may not be
         * mutated when the originating [ListVal] is mutated.
         */
        val inserted: List<E>,
    ) : ListValChangeEvent<E>()

    class ElementChange<E>(
        override val index: Int,
        val updated: E,
    ) : ListValChangeEvent<E>()
}

typealias ListValObserver<E> = (change: ListValChangeEvent<E>) -> Unit
