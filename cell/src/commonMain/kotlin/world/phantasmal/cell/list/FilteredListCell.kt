package world.phantasmal.cell.list

import world.phantasmal.cell.Cell
import world.phantasmal.cell.ChangeEvent
import world.phantasmal.cell.Dependency
import world.phantasmal.cell.Dependent
import world.phantasmal.core.assert
import world.phantasmal.core.assertUnreachable
import world.phantasmal.core.unsafe.unsafeCast

internal class FilteredListCell<E>(
    list: ListCell<E>,
    private val predicate: Cell<(E) -> Cell<Boolean>>,
) : AbstractFilteredListCell<E>(list) {
    /**
     * Maps the dependency's indices to the corresponding index into this list and the result of the
     * predicate applied to the element at that index. When an element of the dependency list
     * doesn't pass the predicate, its index in this mapping is set to -1.
     */
    private val indexMap = mutableListOf<Mapping>()

    private val changedPredicateResults = mutableListOf<Mapping>()

    override val predicateDependency: Dependency<*>
        get() = predicate

    override fun removeDependent(dependent: Dependent) {
        super.removeDependent(dependent)

        if (dependents.isEmpty()) {
            for (mapping in indexMap) {
                mapping.removeDependent(this)
            }
        }
    }

    override fun otherDependencyInvalidated(dependency: Dependency<*>) {
        assert(
            { dependency is FilteredListCell<*>.Mapping },
            { "Expected $dependency to be a mapping." },
        )

        changedPredicateResults.add(unsafeCast(dependency))
    }

    override fun ignoreOtherChanges() {
        changedPredicateResults.clear()
    }

    override fun processOtherChanges(filteredChanges: MutableList<ListChange<E>>) {
        var shift = 0

        for ((dependencyIndex, mapping) in indexMap.withIndex()) {
            if (changedPredicateResults.isEmpty()) {
                break
            }

            if (changedPredicateResults.remove(mapping)) {
                val result = mapping.predicateResult.value
                val oldResult = mapping.index != -1

                if (result != oldResult) {
                    val prevSize = elements.size

                    if (result) {
                        // TODO: Avoid this loop by storing the index where an element "would" be
                        //       if it passed the predicate.
                        var insertionIndex = elements.size

                        for (index in (dependencyIndex + 1)..indexMap.lastIndex) {
                            val i = indexMap[index].index

                            if (i != -1) {
                                insertionIndex = i + shift
                                break
                            }
                        }

                        val element = list.value[dependencyIndex]
                        elements.add(insertionIndex, element)
                        mapping.index = insertionIndex
                        shift++

                        filteredChanges.add(
                            ListChange(
                                insertionIndex,
                                prevSize,
                                removed = emptyList(),
                                inserted = listOf(element),
                            )
                        )
                    } else {
                        val index = mapping.index + shift
                        val element = elements.removeAt(index)
                        mapping.index = -1
                        shift--

                        filteredChanges.add(
                            ListChange(
                                index,
                                prevSize,
                                removed = listOf(element),
                                inserted = emptyList(),
                            )
                        )
                    }
                } else if (oldResult) {
                    mapping.index += shift
                }
            } else {
                mapping.index += shift
            }
        }

        // Can still contain changed mappings at this point if e.g. an element was removed after its
        // predicate result changed or a predicate result emitted multiple invalidation
        // notifications.
        changedPredicateResults.clear()
    }

    override fun applyPredicate(element: E): Boolean =
        predicate.value(element).value

    override fun maxDepIndex(): Int =
        indexMap.lastIndex

    override fun mapIndex(index: Int): Int =
        indexMap[index].index

    override fun removeIndexMapping(index: Int): Int {
        val mapping = indexMap.removeAt(index)
        mapping.removeDependent(this)
        return mapping.index
    }

    override fun insertIndexMapping(depIndex: Int, localIndex: Int, element: E) {
        val mapping = Mapping(predicate.value(element), localIndex)
        mapping.addDependent(this)
        indexMap.add(depIndex, mapping)
    }

    override fun shiftIndexMapping(depIndex: Int, shift: Int) {
        val mapping = indexMap[depIndex]

        if (mapping.index != -1) {
            mapping.index += shift
        }
    }

    override fun recompute() {
        elements.clear()

        for (mapping in indexMap) {
            mapping.removeDependent(this)
        }

        indexMap.clear()

        // Cache value here to facilitate loop unswitching.
        val hasDependents = dependents.isNotEmpty()
        val pred = predicate.value

        for (element in list.value) {
            val predicateResult = pred(element)

            val index =
                if (predicateResult.value) {
                    elements.add(element)
                    elements.lastIndex
                } else {
                    -1
                }

            if (hasDependents) {
                val mapping = Mapping(predicateResult, index)
                mapping.addDependent(this)
                indexMap.add(mapping)
            }
        }
    }

    override fun resetChangeWaveData() {
        changedPredicateResults.clear()
    }

    private inner class Mapping(
        val predicateResult: Cell<Boolean>,
        /**
         * The index into [elements] if the element passes the predicate. -1 If the element does not
         * pass the predicate.
         */
        var index: Int,
    ) : Dependent, Dependency<Boolean> {
        override val changeEvent: ChangeEvent<Boolean>?
            get() {
                assertUnreachable { "Change event is never computed." }
                return null
            }

        override fun dependencyInvalidated(dependency: Dependency<*>) {
            assert { dependency === predicateResult }

            this@FilteredListCell.dependencyInvalidated(this)
        }

        override fun addDependent(dependent: Dependent) {
            assert { dependent === this@FilteredListCell }

            predicateResult.addDependent(this)
        }

        override fun removeDependent(dependent: Dependent) {
            assert { dependent === this@FilteredListCell }

            predicateResult.removeDependent(this)
        }
    }
}
