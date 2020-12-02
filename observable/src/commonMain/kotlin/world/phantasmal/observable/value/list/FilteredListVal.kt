package world.phantasmal.observable.value.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.observable.Observer
import world.phantasmal.observable.value.AbstractVal
import world.phantasmal.observable.value.Val

// TODO: This class shares 95% of its code with DependentListVal.
class FilteredListVal<E>(
    private val dependency: ListVal<E>,
    private val predicate: (E) -> Boolean,
) : AbstractListVal<E>(mutableListOf(), extractObservables = null) {
    private val _sizeVal = SizeVal()

    /**
     * Set to true right before actual observers are added.
     */
    private var hasObservers = false

    private var dependencyObserver: Disposable? = null

    override val value: List<E>
        get() {
            if (!hasObservers) {
                recompute()
            }

            return elements
        }

    override val sizeVal: Val<Int> = _sizeVal

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
        elements.clear()
        dependency.value.filterTo(elements, predicate)
    }

    private fun initDependencyObservers() {
        if (dependencyObserver == null) {
            hasObservers = true

            dependencyObserver = dependency.observeList { event ->
                when (event) {
                    is ListValChangeEvent.Change -> {
                        var index = 0

                        repeat(event.index) { i ->
                            if (predicate(dependency[i])) {
                                index++
                            }
                        }

                        val removed = mutableListOf<E>()

                        event.removed.forEach { element ->
                            if (predicate(element)) {
                                removed.add(elements.removeAt(index))
                            }
                        }

                        val inserted = event.inserted.filter(predicate)
                        elements.addAll(index, inserted)

                        if (removed.isNotEmpty() || inserted.isNotEmpty()) {
                            finalizeUpdate(ListValChangeEvent.Change(index, removed, inserted))
                        }
                    }

                    is ListValChangeEvent.ElementChange -> {
                        finalizeUpdate(event)
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
