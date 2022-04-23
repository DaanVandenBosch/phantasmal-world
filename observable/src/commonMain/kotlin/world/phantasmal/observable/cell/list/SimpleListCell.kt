package world.phantasmal.observable.cell.list

import world.phantasmal.core.replaceAll
import world.phantasmal.observable.ChangeManager

/**
 * @param elements The backing list for this [ListCell].
 */
class SimpleListCell<E>(
    override val elements: MutableList<E>,
) : AbstractListCell<E>(), MutableListCell<E> {

    private var changes = mutableListOf<ListChange<E>>()

    override var value: List<E>
        get() = elementsWrapper
        set(value) {
            replaceAll(value)
        }

    override operator fun get(index: Int): E =
        elements[index]

    override operator fun set(index: Int, element: E): E {
        checkIndex(index, elements.lastIndex)
        emitMightChange()

        copyAndResetWrapper()
        val removed = elements.set(index, element)

        finalizeChange(
            index,
            prevSize = elements.size,
            removed = listOf(removed),
            inserted = listOf(element),
        )

        return removed
    }

    override fun add(element: E) {
        emitMightChange()

        val index = elements.size
        copyAndResetWrapper()
        elements.add(element)

        finalizeChange(
            index,
            prevSize = index,
            removed = emptyList(),
            inserted = listOf(element),
        )
    }

    override fun add(index: Int, element: E) {
        val prevSize = elements.size
        checkIndex(index, prevSize)
        emitMightChange()

        copyAndResetWrapper()
        elements.add(index, element)

        finalizeChange(index, prevSize, removed = emptyList(), inserted = listOf(element))
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
        checkIndex(index, elements.lastIndex)
        emitMightChange()

        val prevSize = elements.size

        copyAndResetWrapper()
        val removed = elements.removeAt(index)

        finalizeChange(index, prevSize, removed = listOf(removed), inserted = emptyList())
        return removed
    }

    override fun replaceAll(elements: Iterable<E>) {
        emitMightChange()

        val prevSize = this.elements.size
        val removed = elementsWrapper

        copyAndResetWrapper()
        this.elements.replaceAll(elements)

        finalizeChange(index = 0, prevSize, removed, inserted = elementsWrapper)
    }

    override fun replaceAll(elements: Sequence<E>) {
        emitMightChange()

        val prevSize = this.elements.size
        val removed = elementsWrapper

        copyAndResetWrapper()
        this.elements.replaceAll(elements)

        finalizeChange(index = 0, prevSize, removed, inserted = elementsWrapper)
    }

    override fun splice(fromIndex: Int, removeCount: Int, newElement: E) {
        val prevSize = elements.size
        val removed = ArrayList<E>(removeCount)

        for (i in fromIndex until (fromIndex + removeCount)) {
            removed.add(elements[i])
        }

        emitMightChange()

        copyAndResetWrapper()
        repeat(removeCount) { elements.removeAt(fromIndex) }
        elements.add(fromIndex, newElement)

        finalizeChange(fromIndex, prevSize, removed, inserted = listOf(newElement))
    }

    override fun clear() {
        emitMightChange()

        val prevSize = elements.size
        val removed = elementsWrapper

        copyAndResetWrapper()
        elements.clear()

        finalizeChange(index = 0, prevSize, removed, inserted = emptyList())
    }

    override fun sortWith(comparator: Comparator<E>) {
        emitMightChange()

        val removed = elementsWrapper
        copyAndResetWrapper()
        var throwable: Throwable? = null

        try {
            elements.sortWith(comparator)
        } catch (e: Throwable) {
            throwable = e
        }

        finalizeChange(
            index = 0,
            prevSize = elements.size,
            removed,
            inserted = elementsWrapper,
        )

        if (throwable != null) {
            throw throwable
        }
    }

    override fun emitDependencyChanged() {
        val currentChanges = changes
        changes = mutableListOf()
        emitDependencyChangedEvent(ListChangeEvent(elementsWrapper, currentChanges))
    }

    private fun checkIndex(index: Int, maxIndex: Int) {
        if (index !in 0..maxIndex) {
            throw IndexOutOfBoundsException(
                "Index $index out of bounds for length ${elements.size}",
            )
        }
    }

    private fun finalizeChange(
        index: Int,
        prevSize: Int,
        removed: List<E>,
        inserted: List<E>,
    ) {
        changes.add(ListChange(index, prevSize, removed, inserted))
        ChangeManager.changed(this)
    }
}
