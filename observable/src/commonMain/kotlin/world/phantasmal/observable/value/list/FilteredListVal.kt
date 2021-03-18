package world.phantasmal.observable.value.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.observable.Observer
import world.phantasmal.observable.value.AbstractVal
import world.phantasmal.observable.value.Val

// TODO: This class shares 95% of its code with AbstractDependentListVal.
class FilteredListVal<E>(
    private val dependency: ListVal<E>,
    private val predicate: (E) -> Boolean,
) : AbstractListVal<E>(extractObservables = null) {
    private val _sizeVal = SizeVal()

    /**
     * Set to true right before actual observers are added.
     */
    private var hasObservers = false

    private var dependencyObserver: Disposable? = null

    /**
     * Maps the dependency's indices to this list's indices. When an element of the dependency list
     * doesn't pass the predicate, it's index in this mapping is set to -1.
     */
    private val indexMap = mutableListOf<Int>()

    private var elements: ListWrapper<E> = ListWrapper(mutableListOf())

    override val value: List<E>
        get() {
            if (!hasObservers) {
                recompute()
            }

            return elements
        }

    override val size: Val<Int> = _sizeVal

    override fun observe(callNow: Boolean, observer: Observer<List<E>>): Disposable {
        initDependencyObservers()

        val superDisposable = super.observe(callNow, observer)

        return disposable {
            superDisposable.dispose()
            disposeDependencyObservers()
        }
    }

    override fun observeList(callNow: Boolean, observer: ListValObserver<E>): Disposable {
        initDependencyObservers()

        val superDisposable = super.observeList(callNow, observer)

        return disposable {
            superDisposable.dispose()
            disposeDependencyObservers()
        }
    }

    private fun recompute() {
        elements = ListWrapper(mutableListOf())
        indexMap.clear()

        dependency.value.forEach { element ->
            if (predicate(element)) {
                elements.mutate { add(element) }
                indexMap.add(elements.lastIndex)
            } else {
                indexMap.add(-1)
            }
        }
    }

    private fun initDependencyObservers() {
        if (dependencyObserver == null) {
            hasObservers = true

            dependencyObserver = dependency.observeList { event ->
                when (event) {
                    is ListValChangeEvent.Change -> {
                        // Figure out which elements should be removed from this list, then simply
                        // recompute the entire filtered list and finally figure out which elements
                        // have been added. Emit a Change event if something actually changed.
                        val removed = mutableListOf<E>()
                        var eventIndex = -1

                        event.removed.forEachIndexed { i, element ->
                            val index = indexMap[event.index + i]

                            if (index != -1) {
                                removed.add(element)

                                if (eventIndex == -1) {
                                    eventIndex = index
                                }
                            }
                        }

                        recompute()

                        val inserted = mutableListOf<E>()

                        event.inserted.forEachIndexed { i, element ->
                            val index = indexMap[event.index + i]

                            if (index != -1) {
                                inserted.add(element)

                                if (eventIndex == -1) {
                                    eventIndex = index
                                }
                            }
                        }

                        if (removed.isNotEmpty() || inserted.isNotEmpty()) {
                            check(eventIndex != -1)
                            finalizeUpdate(ListValChangeEvent.Change(eventIndex, removed, inserted))
                        }
                    }

                    is ListValChangeEvent.ElementChange -> {
                        // Emit a Change or ElementChange event based on whether the updated element
                        // passes the predicate test and whether it was already in the elements list
                        // (i.e. whether it passed the predicate test before the update).
                        val index = indexMap[event.index]

                        if (predicate(event.updated)) {
                            if (index == -1) {
                                // If the element now passed the test and previously didn't pass,
                                // insert it and emit a Change event.
                                var insertIndex = elements.size

                                for (depIdx in (event.index + 1)..indexMap.lastIndex) {
                                    val thisIdx = indexMap[depIdx]

                                    if (thisIdx != -1) {
                                        insertIndex = thisIdx
                                        break
                                    }
                                }

                                elements = elements.mutate { add(insertIndex, event.updated) }
                                indexMap[event.index] = insertIndex

                                for (depIdx in (event.index + 1)..indexMap.lastIndex) {
                                    val thisIdx = indexMap[depIdx]

                                    if (thisIdx != -1) {
                                        indexMap[depIdx]++
                                    }
                                }

                                finalizeUpdate(ListValChangeEvent.Change(
                                    insertIndex,
                                    emptyList(),
                                    listOf(event.updated),
                                ))
                            } else {
                                // Otherwise just propagate the ElementChange event.
                                finalizeUpdate(
                                    ListValChangeEvent.ElementChange(index, event.updated)
                                )
                            }
                        } else {
                            if (index != -1) {
                                // If the element now doesn't pass the test and it previously did
                                // pass, remove it and emit a Change event.
                                elements = elements.mutate { removeAt(index) }
                                indexMap[event.index] = -1

                                for (depIdx in (event.index + 1)..indexMap.lastIndex) {
                                    val thisIdx = indexMap[depIdx]

                                    if (thisIdx != -1) {
                                        indexMap[depIdx]--
                                    }
                                }

                                finalizeUpdate(ListValChangeEvent.Change(
                                    index,
                                    listOf(event.updated),
                                    emptyList(),
                                ))
                            } else {
                                // Otherwise just propagate the ElementChange event.
                                finalizeUpdate(
                                    ListValChangeEvent.ElementChange(index, event.updated)
                                )
                            }
                        }
                    }
                }
            }

            recompute()
        }
    }

    private fun disposeDependencyObservers() {
        if (observers.isEmpty() && listObservers.isEmpty() && _sizeVal.publicObservers.isEmpty()) {
            hasObservers = false
            dependencyObserver?.dispose()
            dependencyObserver = null
        }
    }

    override fun finalizeUpdate(event: ListValChangeEvent<E>) {
        if (event is ListValChangeEvent.Change && event.removed.size != event.inserted.size) {
            _sizeVal.publicEmit()
        }

        super.finalizeUpdate(event)
    }

    private inner class SizeVal : AbstractVal<Int>() {
        override val value: Int
            get() {
                if (!hasObservers) {
                    recompute()
                }

                return elements.size
            }

        val publicObservers = super.observers

        override fun observe(callNow: Boolean, observer: Observer<Int>): Disposable {
            initDependencyObservers()

            val superDisposable = super.observe(callNow, observer)

            return disposable {
                superDisposable.dispose()
                disposeDependencyObservers()
            }
        }

        fun publicEmit() {
            super.emit()
        }
    }
}
