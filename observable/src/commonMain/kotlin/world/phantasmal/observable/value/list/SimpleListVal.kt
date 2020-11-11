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
) : AbstractListVal<E>(elements, extractObservables), MutableListVal<E> {
    private val _sizeVal: MutableVal<Int> = mutableVal(elements.size)

    override var value: List<E> = elements
        set(value) {
            replaceAll(value)
        }

    override val sizeVal: Val<Int> = _sizeVal

    override operator fun get(index: Int): E =
        elements[index]

    override operator fun set(index: Int, element: E): E {
        val removed = elements.set(index, element)
        finalizeUpdate(ListValChangeEvent.Change(index, listOf(removed), listOf(element)))
        return removed
    }

    override fun add(element: E) {
        val index = elements.size
        elements.add(element)
        finalizeUpdate(ListValChangeEvent.Change(index, emptyList(), listOf(element)))
    }

    override fun add(index: Int, element: E) {
        elements.add(index, element)
        finalizeUpdate(ListValChangeEvent.Change(index, emptyList(), listOf(element)))
    }

    override fun removeAt(index: Int): E {
        val removed = elements.removeAt(index)
        finalizeUpdate(ListValChangeEvent.Change(index, listOf(removed), emptyList()))
        return removed
    }

    override fun replaceAll(elements: Iterable<E>) {
        val removed = ArrayList(this.elements)
        this.elements.clear()
        this.elements.addAll(elements)
        finalizeUpdate(ListValChangeEvent.Change(0, removed, this.elements))
    }

    override fun replaceAll(elements: Sequence<E>) {
        val removed = ArrayList(this.elements)
        this.elements.clear()
        this.elements.addAll(elements)
        finalizeUpdate(ListValChangeEvent.Change(0, removed, this.elements))
    }

    override fun splice(from: Int, removeCount: Int, newElement: E) {
        val removed = ArrayList(elements.subList(from, from + removeCount))
        repeat(removeCount) { elements.removeAt(from) }
        elements.add(from, newElement)
        finalizeUpdate(ListValChangeEvent.Change(from, removed, listOf(newElement)))
    }

    override fun clear() {
        val removed = ArrayList(elements)
        elements.clear()
        finalizeUpdate(ListValChangeEvent.Change(0, removed, emptyList()))
    }

    override fun finalizeUpdate(event: ListValChangeEvent<E>) {
        _sizeVal.value = elements.size
        super.finalizeUpdate(event)
    }
}
