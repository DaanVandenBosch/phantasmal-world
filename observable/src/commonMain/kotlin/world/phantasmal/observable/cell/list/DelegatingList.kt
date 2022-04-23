package world.phantasmal.observable.cell.list

/**
 * Simply delegates all methods to [backingList], even [equals], [hashCode] and [toString].
 */
class DelegatingList<E>(var backingList: List<E>) : List<E> {
    override val size: Int = backingList.size

    override fun contains(element: E): Boolean = backingList.contains(element)

    override fun containsAll(elements: Collection<E>): Boolean = backingList.containsAll(elements)

    override fun get(index: Int): E = backingList[index]

    override fun indexOf(element: E): Int = backingList.indexOf(element)

    override fun isEmpty(): Boolean = backingList.isEmpty()

    override fun iterator(): Iterator<E> = backingList.iterator()

    override fun lastIndexOf(element: E): Int = backingList.lastIndexOf(element)

    override fun listIterator(): ListIterator<E> = backingList.listIterator()

    override fun listIterator(index: Int): ListIterator<E> = backingList.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int): List<E> =
        backingList.subList(fromIndex, toIndex)

    override fun equals(other: Any?): Boolean = other == backingList

    override fun hashCode(): Int = backingList.hashCode()

    override fun toString(): String = backingList.toString()
}
