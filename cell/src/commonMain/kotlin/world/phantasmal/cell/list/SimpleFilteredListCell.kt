package world.phantasmal.cell.list

import world.phantasmal.core.assertUnreachable
import world.phantasmal.cell.Dependency
import world.phantasmal.cell.Cell

internal class SimpleFilteredListCell<E>(
    list: ListCell<E>,
    private val predicate: Cell<(E) -> Boolean>,
) : AbstractFilteredListCell<E>(list) {
    /**
     * Maps the dependency's indices to this list's indices. When an element of the dependency list
     * doesn't pass the predicate, its index in this mapping is set to -1.
     *
     * This is not a performance improvement but a requirement. We can't determine an element's
     * index into our own [elements] list by using e.g. indexOf because a list can contain the same
     * element multiple times.
     */
    private val indexMap = mutableListOf<Int>()

    override val predicateDependency: Dependency<*>
        get() = predicate

    override fun otherDependencyInvalidated(dependency: Dependency<*>) {
        assertUnreachable { "Unexpected dependency $dependency." }
    }

    override fun ignoreOtherChanges() {
        // Nothing to ignore.
    }

    override fun processOtherChanges(filteredChanges: MutableList<ListChange<E>>) {
        // Nothing to process.
    }

    override fun applyPredicate(element: E): Boolean =
        predicate.value(element)

    override fun maxDepIndex(): Int =
        indexMap.lastIndex

    override fun mapIndex(index: Int): Int =
        indexMap[index]

    override fun removeIndexMapping(index: Int): Int =
        indexMap.removeAt(index)

    override fun insertIndexMapping(depIndex: Int, localIndex: Int, element: E) {
        indexMap.add(depIndex, localIndex)
    }

    override fun shiftIndexMapping(depIndex: Int, shift: Int) {
        val i = indexMap[depIndex]

        if (i != -1) {
            indexMap[depIndex] = i + shift
        }
    }

    override fun recompute() {
        elements.clear()
        indexMap.clear()

        val pred = predicate.value

        for (element in list.value) {
            if (pred(element)) {
                elements.add(element)
                indexMap.add(elements.lastIndex)
            } else {
                indexMap.add(-1)
            }
        }
    }

    override fun resetChangeWaveData() {
        // Nothing to reset.
    }
}
