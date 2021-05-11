package world.phantasmal.observable.cell.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.observable.Observer
import world.phantasmal.observable.cell.AbstractCell
import world.phantasmal.observable.cell.Cell

/**
 * Starts observing its dependencies when the first observer on this cell is registered. Stops
 * observing its dependencies when the last observer on this cell is disposed. This way no extra
 * disposables need to be managed when e.g. [map] is used.
 */
abstract class AbstractDependentListCell<E>(
    private vararg val dependencies: Cell<*>,
) : AbstractListCell<E>(extractObservables = null) {
    private val _size = SizeCell()

    /**
     * Is either empty or has a disposable per dependency.
     */
    private val dependencyObservers = mutableListOf<Disposable>()

    protected abstract val elements: List<E>

    /**
     * Set to true right before actual observers are added.
     */
    protected var hasObservers = false

    override val value: List<E>
        get() {
            if (!hasObservers) {
                computeElements()
            }

            return elements
        }

    override val size: Cell<Int> = _size

    override fun observe(callNow: Boolean, observer: Observer<List<E>>): Disposable {
        initDependencyObservers()

        val superDisposable = super.observe(callNow, observer)

        return disposable {
            superDisposable.dispose()
            disposeDependencyObservers()
        }
    }

    override fun observeList(callNow: Boolean, observer: ListObserver<E>): Disposable {
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
                        val removed = elements
                        computeElements()
                        finalizeUpdate(ListChangeEvent.Change(0, removed, elements))
                    }
                )
            }

            computeElements()
        }
    }

    private fun disposeDependencyObservers() {
        if (observers.isEmpty() && listObservers.isEmpty() && _size.publicObservers.isEmpty()) {
            hasObservers = false
            lastObserverRemoved()
        }
    }

    override fun finalizeUpdate(event: ListChangeEvent<E>) {
        if (event is ListChangeEvent.Change && event.removed.size != event.inserted.size) {
            _size.publicEmit()
        }

        super.finalizeUpdate(event)
    }

    private inner class SizeCell : AbstractCell<Int>() {
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
