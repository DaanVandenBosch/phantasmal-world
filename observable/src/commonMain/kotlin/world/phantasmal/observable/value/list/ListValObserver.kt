package world.phantasmal.observable.value.list

sealed class ListValChangeEvent<E> {
    class Change<E>(
        val index: Int,
        val removed: List<E>,
        val inserted: List<E>
    ) : ListValChangeEvent<E>()

    class ElementChange<E>(
        val index: Int,
        val updated: List<E>
    ) : ListValChangeEvent<E>()
}

typealias ListValObserver<E> = (change: ListValChangeEvent<E>) -> Unit
