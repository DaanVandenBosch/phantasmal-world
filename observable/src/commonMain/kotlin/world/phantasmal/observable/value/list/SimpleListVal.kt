package world.phantasmal.observable.value.list

import world.phantasmal.observable.Observable
import world.phantasmal.observable.value.MutableVal
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal

typealias ObservablesExtractor<E> = (element: E) -> Array<Observable<*>>

/**
 * @param elements The backing list for this ListVal
 * @param extractObservables Extractor function called on each element in this list, changes to the
 * returned observables will be propagated via ElementChange events
 */
class SimpleListVal<E>(
    elements: MutableList<E>,
    extractObservables: ObservablesExtractor<E>? = null,
) : AbstractListVal<E>(extractObservables), MutableListVal<E> {
    private var elements = ListWrapper(elements)
    private val _sizeVal: MutableVal<Int> = mutableVal(elements.size)

    override var value: List<E>
        get() = elements
        set(value) {
            replaceAll(value)
        }

    override val size: Val<Int> = _sizeVal

    override operator fun get(index: Int): E =
        elements[index]

    override operator fun set(index: Int, element: E): E {
        val removed: E
        elements = elements.mutate { removed = set(index, element) }
        finalizeUpdate(ListValChangeEvent.Change(index, listOf(removed), listOf(element)))
        return removed
    }

    override fun add(element: E) {
        val index = elements.size
        elements = elements.mutate { add(index, element) }
        finalizeUpdate(ListValChangeEvent.Change(index, emptyList(), listOf(element)))
    }

    override fun add(index: Int, element: E) {
        elements = elements.mutate { add(index, element) }
        finalizeUpdate(ListValChangeEvent.Change(index, emptyList(), listOf(element)))
    }

    override fun remove(element: E): Boolean {
        val index = elements.indexOf(element)

        return if (index != -1) {
            removeAt(index)
            true
        } else {
            false
        }
    }

    override fun removeAt(index: Int): E {
        val removed: E
        elements = elements.mutate { removed = removeAt(index) }
        finalizeUpdate(ListValChangeEvent.Change(index, listOf(removed), emptyList()))
        return removed
    }

    override fun replaceAll(elements: Iterable<E>) {
        val removed = this.elements
        this.elements = ListWrapper(elements.toMutableList())
        finalizeUpdate(ListValChangeEvent.Change(0, removed, this.elements))
    }

    override fun replaceAll(elements: Sequence<E>) {
        val removed = this.elements
        this.elements = ListWrapper(elements.toMutableList())
        finalizeUpdate(ListValChangeEvent.Change(0, removed, this.elements))
    }

    override fun splice(from: Int, removeCount: Int, newElement: E) {
        val removed = ArrayList(elements.subList(from, from + removeCount))
        elements = elements.mutate {
            repeat(removeCount) { removeAt(from) }
            add(from, newElement)
        }
        finalizeUpdate(ListValChangeEvent.Change(from, removed, listOf(newElement)))
    }

    override fun clear() {
        val removed = elements
        elements = ListWrapper(mutableListOf())
        finalizeUpdate(ListValChangeEvent.Change(0, removed, emptyList()))
    }

    override fun sortWith(comparator: Comparator<E>) {
        elements = elements.mutate { sortWith(comparator) }
        finalizeUpdate(ListValChangeEvent.Change(0, elements, elements))
    }

    override fun finalizeUpdate(event: ListValChangeEvent<E>) {
        _sizeVal.value = elements.size
        super.finalizeUpdate(event)
    }
}
