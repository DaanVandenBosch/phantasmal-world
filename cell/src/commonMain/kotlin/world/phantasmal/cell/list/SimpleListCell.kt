package world.phantasmal.cell.list

import world.phantasmal.cell.MutationManager
import world.phantasmal.core.replaceAll
import world.phantasmal.core.unsafe.unsafeCast

/**
 * @param elements The backing list for this [ListCell].
 */
class SimpleListCell<E>(
    override val elements: MutableList<E>,
) : AbstractElementsWrappingListCell<E>(), MutableListCell<E> {

    override var value: List<E>
        get() = elementsWrapper
        set(value) {
            replaceAll(value)
        }

    override var changeEvent: ListChangeEvent<E>? = null
        private set

    /** Mutation ID during which the current list of changes was created. */
    private var changesMutationId: Long = -1

    override operator fun get(index: Int): E =
        elements[index]

    override operator fun set(index: Int, element: E): E {
        checkIndex(index, elements.lastIndex)

        applyChange {
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
    }

    override fun add(element: E) {
        applyChange {
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
    }

    override fun add(index: Int, element: E) {
        val prevSize = elements.size
        checkIndex(index, prevSize)

        applyChange {
            copyAndResetWrapper()
            elements.add(index, element)

            finalizeChange(index, prevSize, removed = emptyList(), inserted = listOf(element))
        }
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

        applyChange {
            val prevSize = elements.size

            copyAndResetWrapper()
            val removed = elements.removeAt(index)

            finalizeChange(index, prevSize, removed = listOf(removed), inserted = emptyList())
            return removed
        }
    }

    override fun replaceAll(elements: Iterable<E>) {
        applyChange {
            val prevSize = this.elements.size
            val removed = elementsWrapper

            copyAndResetWrapper()
            this.elements.replaceAll(elements)

            finalizeChange(index = 0, prevSize, removed, inserted = elementsWrapper)
        }
    }

    override fun replaceAll(elements: Sequence<E>) {
        applyChange {
            val prevSize = this.elements.size
            val removed = elementsWrapper

            copyAndResetWrapper()
            this.elements.replaceAll(elements)

            finalizeChange(index = 0, prevSize, removed, inserted = elementsWrapper)
        }
    }

    override fun splice(fromIndex: Int, removeCount: Int, newElement: E) {
        val prevSize = elements.size
        val removed = ArrayList<E>(removeCount)

        // Do this loop outside applyChange because it will throw when any index is out of bounds.
        for (i in fromIndex until (fromIndex + removeCount)) {
            removed.add(elements[i])
        }

        applyChange {
            copyAndResetWrapper()
            repeat(removeCount) { elements.removeAt(fromIndex) }
            elements.add(fromIndex, newElement)

            finalizeChange(fromIndex, prevSize, removed, inserted = listOf(newElement))
        }
    }

    override fun clear() {
        if (elements.isEmpty()) {
            return
        }

        applyChange {
            val prevSize = elements.size
            val removed = elementsWrapper

            copyAndResetWrapper()
            elements.clear()

            finalizeChange(index = 0, prevSize, removed, inserted = emptyList())
        }
    }

    override fun sortWith(comparator: Comparator<E>) {
        applyChange {
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
        val event = changeEvent

        // Reuse the same list of changes during a mutation.
        val changes: MutableList<ListChange<E>> =
            if (event == null || changesMutationId != MutationManager.currentMutationId) {
                changesMutationId = MutationManager.currentMutationId
                mutableListOf()
            } else {
                // This cast is safe because we know we always instantiate our change event with a mutable list.
                unsafeCast(event.changes)
            }

        changes.add(ListChange(index, prevSize, removed, inserted))
        changeEvent = ListChangeEvent(elementsWrapper, changes)
    }
}
