package world.phantasmal.cell.list

import world.phantasmal.cell.Dependency
import world.phantasmal.cell.Dependent
import world.phantasmal.cell.MutationManager
import world.phantasmal.core.unsafe.unsafeCast

internal abstract class AbstractFilteredListCell<E>(
    protected val list: ListCell<E>,
) : AbstractListCell<E>(), Dependent {

    /** Set during a change wave when [list] changes. */
    private var listInvalidated = false

    /** Mutation ID during which the current list of changes was created. */
    private var changesMutationId: Long = -1
    private var listChangeIndex = 0

    /** Set during a change wave when [predicateDependency] changes. */
    private var predicateInvalidated = false

    private var valid = false

    protected val elements = mutableListOf<E>()

    protected abstract val predicateDependency: Dependency<*>

    final override val value: List<E>
        get() {
            computeValueAndEvent()
            return elements
        }

    private var _changeEvent: ListChangeEvent<E>? = null
    final override val changeEvent: ListChangeEvent<E>?
        get() {
            computeValueAndEvent()
            return _changeEvent
        }

    private fun computeValueAndEvent() {
        if (!valid) {
            // Reuse the same list of changes during a mutation.
            val event = _changeEvent
            val filteredChanges: MutableList<ListChange<E>>

            if (event == null || changesMutationId != MutationManager.currentMutationId) {
                changesMutationId = MutationManager.currentMutationId
                listChangeIndex = 0
                filteredChanges = mutableListOf()
                _changeEvent = ListChangeEvent(elements, filteredChanges)
            } else {
                // This cast is safe because we know we always instantiate our change event
                // with a mutable list.
                filteredChanges = unsafeCast(event.changes)
            }

            val hasDependents = dependents.isNotEmpty()

            if (predicateInvalidated || !hasDependents) {
                // Simply assume the entire list changes and recompute.
                val removed = elements.toList()

                ignoreOtherChanges()
                recompute()

                filteredChanges.add(
                    ListChange(index = 0, prevSize = removed.size, removed, elements),
                )
            } else {
                val listChangeEvent = list.changeEvent

                if (listInvalidated && listChangeEvent != null) {
                    for (change in listChangeEvent.changes.listIterator(listChangeIndex)) {
                        val prevSize = elements.size
                        // Map the incoming change index to an index into our own elements list.
                        // TODO: Avoid this loop by storing the index where an element "would"
                        //       be if it passed the predicate?
                        var eventIndex = prevSize

                        for (index in change.index..maxDepIndex()) {
                            val i = mapIndex(index)

                            if (i != -1) {
                                eventIndex = i
                                break
                            }
                        }

                        // Process removals.
                        val removed = mutableListOf<E>()

                        for (element in change.removed) {
                            val index = removeIndexMapping(change.index)

                            if (index != -1) {
                                elements.removeAt(eventIndex)
                                removed.add(element)
                            }
                        }

                        // Process insertions.
                        val inserted = mutableListOf<E>()
                        var insertionIndex = eventIndex

                        for ((i, element) in change.inserted.withIndex()) {
                            if (applyPredicate(element)) {
                                insertIndexMapping(change.index + i, insertionIndex, element)
                                elements.add(insertionIndex, element)
                                inserted.add(element)
                                insertionIndex++
                            } else {
                                insertIndexMapping(change.index + i, -1, element)
                            }
                        }

                        // Shift mapped indices by a certain amount. This amount can be
                        // positive, negative or zero.
                        val diff = inserted.size - removed.size

                        if (diff != 0) {
                            // Indices before the change index stay the same. Newly inserted
                            // indices are already correct. So we only need to shift everything
                            // after the new indices.
                            val startIndex = change.index + change.inserted.size

                            for (index in startIndex..maxDepIndex()) {
                                shiftIndexMapping(index, diff)
                            }
                        }

                        // Add a list change if something actually changed.
                        if (removed.isNotEmpty() || inserted.isNotEmpty()) {
                            filteredChanges.add(
                                ListChange(
                                    eventIndex,
                                    prevSize,
                                    removed,
                                    inserted,
                                )
                            )
                        }
                    }

                    listChangeIndex = listChangeEvent.changes.size
                }

                processOtherChanges(filteredChanges)

                if (filteredChanges.isEmpty()) {
                    _changeEvent = null
                } else {
                    // Keep the previous change event, it has been changed internally.
                }
            }

            // Reset for next change wave.
            listInvalidated = false
            predicateInvalidated = false
            // We stay invalid if we have no dependents to ensure our value is always recomputed.
            valid = hasDependents
        }

        resetChangeWaveData()
    }

    override fun addDependent(dependent: Dependent) {
        super.addDependent(dependent)

        if (dependents.size == 1) {
            list.addDependent(this)
            predicateDependency.addDependent(this)
            recompute()
        }
    }

    override fun removeDependent(dependent: Dependent) {
        super.removeDependent(dependent)

        if (dependents.isEmpty()) {
            valid = false
            predicateDependency.removeDependent(this)
            list.removeDependent(this)
        }
    }

    override fun dependencyInvalidated(dependency: Dependency<*>) {
        valid = false

        if (dependency === list) {
            listInvalidated = true
        } else if (dependency === predicateDependency) {
            predicateInvalidated = true
        } else {
            otherDependencyInvalidated(dependency)
        }

        emitDependencyInvalidated()
    }

    /** Called when a dependency that's neither [list] nor [predicateDependency] has changed. */
    protected abstract fun otherDependencyInvalidated(dependency: Dependency<*>)

    protected abstract fun ignoreOtherChanges()

    protected abstract fun processOtherChanges(filteredChanges: MutableList<ListChange<E>>)

    protected abstract fun applyPredicate(element: E): Boolean

    protected abstract fun maxDepIndex(): Int

    /**
     * Maps and index into [list] to an index into this list. Returns -1 if the given index does not
     * point to an element that passes the predicate, i.e. the element is not in this list.
     */
    protected abstract fun mapIndex(index: Int): Int

    /**
     * Removes the element at the given index into [list] from our mapping. Returns the previous
     * index into our list.
     */
    protected abstract fun removeIndexMapping(index: Int): Int

    protected abstract fun insertIndexMapping(depIndex: Int, localIndex: Int, element: E)

    /** Adds [shift] to the local index at [depIndex] if it's not -1. */
    protected abstract fun shiftIndexMapping(depIndex: Int, shift: Int)

    protected abstract fun recompute()

    protected abstract fun resetChangeWaveData()
}
