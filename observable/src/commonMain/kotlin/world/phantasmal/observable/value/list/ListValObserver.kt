package world.phantasmal.observable.value.list

sealed class ListValChangeEvent<out E> {
    abstract val index: Int

    class Change<E>(
        override val index: Int,
        val removed: List<E>,
        val inserted: List<E>,
    ) : ListValChangeEvent<E>()

    class ElementChange<E>(
        override val index: Int,
        val updated: E,
    ) : ListValChangeEvent<E>()
}

typealias ListValObserver<E> = (change: ListValChangeEvent<E>) -> Unit
