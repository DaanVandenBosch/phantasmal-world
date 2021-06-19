package world.phantasmal.observable.cell.list

import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Dependency
import world.phantasmal.observable.Dependent

class FilteredListCell<E>(
    private val dependency: ListCell<E>,
    private val predicate: (E) -> Boolean,
) : AbstractListCell<E>(), Dependent {
    /**
     * Maps the dependency's indices to this list's indices. When an element of the dependency list
     * doesn't pass the predicate, its index in this mapping is set to -1.
     */
    private val indexMap = mutableListOf<Int>()

    private val elements = mutableListOf<E>()

    override val value: List<E>
        get() {
            if (dependents.isEmpty()) {
                recompute()
            }

            return elements
        }

    override fun addDependent(dependent: Dependent) {
        if (dependents.isEmpty()) {
            dependency.addDependent(this)
            recompute()
        }

        super.addDependent(dependent)
    }

    override fun removeDependent(dependent: Dependent) {
        super.removeDependent(dependent)

        if (dependents.isEmpty()) {
            dependency.removeDependent(this)
        }
    }

    override fun dependencyMightChange() {
        emitMightChange()
    }

    override fun dependencyChanged(dependency: Dependency, event: ChangeEvent<*>?) {
        if (event is ListChangeEvent<*>) {
            val filteredChanges = mutableListOf<ListChange<E>>()

            for (change in event.changes) {
                when (change) {
                    is ListChange.Structural -> {
                        // Figure out which elements should be removed from this list, then simply
                        // recompute the entire filtered list and finally figure out which elements
                        // have been added. Emit a change event if something actually changed.
                        @Suppress("UNCHECKED_CAST")
                        change as ListChange.Structural<E>

                        val removed = mutableListOf<E>()
                        var eventIndex = -1

                        change.removed.forEachIndexed { i, element ->
                            val index = indexMap[change.index + i]

                            if (index != -1) {
                                removed.add(element)

                                if (eventIndex == -1) {
                                    eventIndex = index
                                }
                            }
                        }

                        recompute()

                        val inserted = mutableListOf<E>()

                        change.inserted.forEachIndexed { i, element ->
                            val index = indexMap[change.index + i]

                            if (index != -1) {
                                inserted.add(element)

                                if (eventIndex == -1) {
                                    eventIndex = index
                                }
                            }
                        }

                        if (removed.isNotEmpty() || inserted.isNotEmpty()) {
                            check(eventIndex != -1)
                            filteredChanges.add(
                                ListChange.Structural(
                                    eventIndex,
                                    removed,
                                    inserted
                                )
                            )
                        }
                    }
                    is ListChange.Element -> {
                        // Emit a structural or element change based on whether the updated element
                        // passes the predicate test and whether it was already in the elements list
                        // (i.e. whether it passed the predicate test before the update).
                        @Suppress("UNCHECKED_CAST")
                        change as ListChange.Element<E>

                        val index = indexMap[change.index]

                        if (predicate(change.updated)) {
                            if (index == -1) {
                                // If the element now passed the test and previously didn't pass,
                                // insert it and emit a Change event.
                                var insertIndex = elements.size

                                for (depIdx in (change.index + 1)..indexMap.lastIndex) {
                                    val thisIdx = indexMap[depIdx]

                                    if (thisIdx != -1) {
                                        insertIndex = thisIdx
                                        break
                                    }
                                }

                                elements.add(insertIndex, change.updated)
                                indexMap[change.index] = insertIndex

                                for (depIdx in (change.index + 1)..indexMap.lastIndex) {
                                    val thisIdx = indexMap[depIdx]

                                    if (thisIdx != -1) {
                                        indexMap[depIdx]++
                                    }
                                }

                                filteredChanges.add(
                                    ListChange.Structural(
                                        insertIndex,
                                        removed = emptyList(),
                                        inserted = listOf(change.updated),
                                    )
                                )
                            } else {
                                // Otherwise just propagate the element change.
                                filteredChanges.add(ListChange.Element(index, change.updated))
                            }
                        } else {
                            if (index != -1) {
                                // If the element now doesn't pass the test and it previously did
                                // pass, remove it and emit a structural change.
                                elements.removeAt(index)
                                indexMap[change.index] = -1

                                for (depIdx in (change.index + 1)..indexMap.lastIndex) {
                                    val thisIdx = indexMap[depIdx]

                                    if (thisIdx != -1) {
                                        indexMap[depIdx]--
                                    }
                                }

                                filteredChanges.add(
                                    ListChange.Structural(
                                        index,
                                        removed = listOf(change.updated),
                                        inserted = emptyList(),
                                    )
                                )
                            } else {
                                // Otherwise just propagate the element change.
                                filteredChanges.add(ListChange.Element(index, change.updated))
                            }
                        }
                    }
                }
            }

            if (filteredChanges.isEmpty()) {
                emitDependencyChanged(null)
            } else {
                emitDependencyChanged(ListChangeEvent(elements, filteredChanges))
            }
        } else {
            emitDependencyChanged(null)
        }
    }

    override fun emitDependencyChanged() {
        // Nothing to do because FilteredListCell emits dependencyChanged immediately. We don't
        // defer this operation because FilteredListCell only changes when there is no transaction
        // or the current transaction is being committed.
    }

    private fun recompute() {
        elements.clear()
        indexMap.clear()

        dependency.value.forEach { element ->
            if (predicate(element)) {
                elements.add(element)
                indexMap.add(elements.lastIndex)
            } else {
                indexMap.add(-1)
            }
        }
    }
}
