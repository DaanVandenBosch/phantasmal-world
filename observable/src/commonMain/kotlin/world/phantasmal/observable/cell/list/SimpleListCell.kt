package world.phantasmal.observable.cell.list

import world.phantasmal.observable.Observable
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.MutableCell
import world.phantasmal.observable.cell.mutableCell

typealias ObservablesExtractor<E> = (element: E) -> Array<Observable<*>>

/**
 * @param elements The backing list for this [ListCell]
 * @param extractObservables Extractor function called on each element in this list, changes to the
 * returned observables will be propagated via ElementChange events
 */
class SimpleListCell<E>(
    elements: MutableList<E>,
    extractObservables: ObservablesExtractor<E>? = null,
) : AbstractListCell<E>(extractObservables), MutableListCell<E> {
    private var elements = ListWrapper(elements)
    private val _size: MutableCell<Int> = mutableCell(elements.size)

    override var value: List<E>
        get() = elements
        set(value) {
            replaceAll(value)
        }

    override val size: Cell<Int> = _size

    override operator fun get(index: Int): E =
        elements[index]

    override operator fun set(index: Int, element: E): E {
        val removed: E
        elements = elements.mutate { removed = set(index, element) }
        finalizeUpdate(ListChangeEvent.Change(index, listOf(removed), listOf(element)))
        return removed
    }

    override fun add(element: E) {
        val index = elements.size
        elements = elements.mutate { add(index, element) }
        finalizeUpdate(ListChangeEvent.Change(index, emptyList(), listOf(element)))
    }

    override fun add(index: Int, element: E) {
        elements = elements.mutate { add(index, element) }
        finalizeUpdate(ListChangeEvent.Change(index, emptyList(), listOf(element)))
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
        finalizeUpdate(ListChangeEvent.Change(index, listOf(removed), emptyList()))
        return removed
    }

    override fun replaceAll(elements: Iterable<E>) {
        val removed = this.elements
        this.elements = ListWrapper(elements.toMutableList())
        finalizeUpdate(ListChangeEvent.Change(0, removed, this.elements))
    }

    override fun replaceAll(elements: Sequence<E>) {
        val removed = this.elements
        this.elements = ListWrapper(elements.toMutableList())
        finalizeUpdate(ListChangeEvent.Change(0, removed, this.elements))
    }

    override fun splice(from: Int, removeCount: Int, newElement: E) {
        val removed = ArrayList(elements.subList(from, from + removeCount))
        elements = elements.mutate {
            repeat(removeCount) { removeAt(from) }
            add(from, newElement)
        }
        finalizeUpdate(ListChangeEvent.Change(from, removed, listOf(newElement)))
    }

    override fun clear() {
        val removed = elements
        elements = ListWrapper(mutableListOf())
        finalizeUpdate(ListChangeEvent.Change(0, removed, emptyList()))
    }

    override fun sortWith(comparator: Comparator<E>) {
        elements = elements.mutate { sortWith(comparator) }
        finalizeUpdate(ListChangeEvent.Change(0, elements, elements))
    }

    override fun finalizeUpdate(event: ListChangeEvent<E>) {
        _size.value = elements.size
        super.finalizeUpdate(event)
    }
}
