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
abstract class AbstractDependentListVal<E>(
    private val dependencies: List<Val<*>>,
) : AbstractListVal<E>(extractObservables = null) {
    private val _sizeVal = SizeVal()

    /**
     * Set to true right before actual observers are added.
     */
    protected var hasObservers = false

    /**
     * Is either empty or has a disposable per dependency.
     */
    private val dependencyObservers = mutableListOf<Disposable>()

    override val value: List<E>
        get() {
            if (!hasObservers) {
                computeElements()
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

    protected abstract fun computeElements()

    protected open fun lastObserverRemoved() {
        dependencyObservers.forEach { it.dispose() }
        dependencyObservers.clear()
    }

    private fun initDependencyObservers() {
        if (dependencyObservers.isEmpty()) {
            hasObservers = true

            dependencies.forEach { dependency ->
                dependencyObservers.add(
                    dependency.observe {
                        val removed = ArrayList(elements)
                        computeElements()
                        finalizeUpdate(ListValChangeEvent.Change(0, removed, elements))
                    }
                )
            }

            computeElements()
        }
    }

    private fun disposeDependencyObservers() {
        if (observers.isEmpty() && listObservers.isEmpty() && _sizeVal.publicObservers.isEmpty()) {
            hasObservers = false
            lastObserverRemoved()
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
                    computeElements()
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
