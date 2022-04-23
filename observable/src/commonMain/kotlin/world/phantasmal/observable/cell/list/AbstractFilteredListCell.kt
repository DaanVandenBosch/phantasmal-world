package world.phantasmal.observable.cell.list

import world.phantasmal.core.unsafe.unsafeCast
import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Dependency
import world.phantasmal.observable.Dependent

abstract class AbstractFilteredListCell<E>(
    protected val list: ListCell<E>,
) : AbstractListCell<E>(), Dependent {

    /** Keeps track of number of changing dependencies during a change wave. */
    private var changingDependencies = 0

    /** Set during a change wave when [list] changes. */
    private var listChangeEvent: ListChangeEvent<E>? = null

    /** Set during a change wave when [predicateDependency] changes. */
    private var predicateChanged = false

    override val elements = mutableListOf<E>()

    protected abstract val predicateDependency: Dependency

    override val value: List<E>
        get() {
            if (dependents.isEmpty()) {
                recompute()
            }

            return elementsWrapper
        }

    override fun addDependent(dependent: Dependent) {
        val wasEmpty = dependents.isEmpty()

        super.addDependent(dependent)

        if (wasEmpty) {
            list.addDependent(this)
            predicateDependency.addDependent(this)
            recompute()
        }
    }

    override fun removeDependent(dependent: Dependent) {
        super.removeDependent(dependent)

        if (dependents.isEmpty()) {
            predicateDependency.removeDependent(this)
            list.removeDependent(this)
        }
    }

    override fun dependencyMightChange() {
        changingDependencies++
        emitMightChange()
    }

    override fun dependencyChanged(dependency: Dependency, event: ChangeEvent<*>?) {
        if (dependency === list) {
            listChangeEvent = unsafeCast(event)
        } else if (dependency === predicateDependency) {
            predicateChanged = event != null
        } else if (event != null) {
            otherDependencyChanged(dependency)
        }

        changingDependencies--

        if (changingDependencies == 0) {
            if (predicateChanged) {
                // Simply assume the entire list changes and recompute.
                val removed = elementsWrapper

                ignoreOtherChanges()
                recompute()

                emitDependencyChangedEvent(
                    ListChangeEvent(
                        elementsWrapper,
                        listOf(ListChange(0, removed.size, removed, elementsWrapper)),
                    )
                )
            } else {
                // TODO: Conditionally copyAndResetWrapper?
                copyAndResetWrapper()
                val filteredChanges = mutableListOf<ListChange<E>>()

                val listChangeEvent = this.listChangeEvent

                if (listChangeEvent != null) {
                    for (change in listChangeEvent.changes) {
                        val prevSize = elements.size
                        // Map the incoming change index to an index into our own elements list.
                        // TODO: Avoid this loop by storing the index where an element "would" be
                        //       if it passed the predicate.
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

                        // Shift mapped indices by a certain amount. This amount can be positive,
                        // negative or zero.
                        val diff = inserted.size - removed.size

                        if (diff != 0) {
                            // Indices before the change index stay the same. Newly inserted indices
                            // are already correct. So we only need to shift everything after the
                            // new indices.
                            for (index in (change.index + change.inserted.size)..maxDepIndex()) {
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
                }

                processOtherChanges(filteredChanges)

                if (filteredChanges.isEmpty()) {
                    emitDependencyChangedEvent(null)
                } else {
                    emitDependencyChangedEvent(
                        ListChangeEvent(elementsWrapper, filteredChanges)
                    )
                }
            }

            // Reset for next change wave.
            listChangeEvent = null
            predicateChanged = false
            resetChangeWaveData()
        }
    }

    /** Called when a dependency that's neither [list] nor [predicateDependency] has changed. */
    protected abstract fun otherDependencyChanged(dependency: Dependency)

    protected abstract fun ignoreOtherChanges()

    protected abstract fun processOtherChanges(filteredChanges: MutableList<ListChange<E>>)

    override fun emitDependencyChanged() {
        // Nothing to do because AbstractFilteredListCell emits dependencyChanged immediately. We
        // don't defer this operation because AbstractFilteredListCell only changes when there is no
        // change set or the current change set is being completed.
    }

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
