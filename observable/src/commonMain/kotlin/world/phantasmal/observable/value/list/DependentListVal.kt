package world.phantasmal.observable.value.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.observable.Observer
import world.phantasmal.observable.value.AbstractVal
import world.phantasmal.observable.value.Val

/**
 * Starts observing its dependencies when the first observer on this property is registered.
 * Stops observing its dependencies when the last observer on this property is disposed.
 * This way no extra disposables need to be managed when e.g. [map] is used.
 */
class DependentListVal<E>(
    private val dependencies: List<Val<*>>,
    private val computeElements: () -> List<E>,
) : AbstractListVal<E>(mutableListOf(), extractObservables = null) {
    private val _sizeVal = SizeVal()

    /**
     * Set to true right before actual observers are added.
     */
    private var hasObservers = false

    /**
     * Is either empty or has a disposable per dependency.
     */
    private val dependencyObservers = mutableListOf<Disposable>()

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
        elements.addAll(computeElements())
    }

    private fun initDependencyObservers() {
        if (dependencyObservers.isEmpty()) {
            hasObservers = true

            dependencies.forEach { dependency ->
                dependencyObservers.add(
                    dependency.observe {
                        val removed = ArrayList(elements)
                        recompute()
                        finalizeUpdate(ListValChangeEvent.Change(0, removed, elements))
                    }
                )
            }

            recompute()
        }
    }

    private fun disposeDependencyObservers() {
        if (observers.isEmpty() && listObservers.isEmpty() && _sizeVal.publicObservers.isEmpty()) {
            hasObservers = false
            dependencyObservers.forEach { it.dispose() }
            dependencyObservers.clear()
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
