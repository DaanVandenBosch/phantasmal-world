package world.phantasmal.observable.value.list

import world.phantasmal.observable.value.MutableVal

interface MutableListVal<E> : ListVal<E>, MutableVal<List<E>> {
    operator fun set(index: Int, element: E): E

    fun add(element: E)

    fun add(index: Int, element: E)

    fun remove(element: E): Boolean

    fun removeAt(index: Int): E

    fun replaceAll(elements: Iterable<E>)

    fun replaceAll(elements: Sequence<E>)

    fun splice(from: Int, removeCount: Int, newElement: E)

    fun clear()
}
