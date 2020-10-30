package world.phantasmal.observable.value

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.core.unsafeToNonNull

/**
 * Starts observing its dependencies when the first observer on this val is registered. Stops
 * observing its dependencies when the last observer on this val is disposed. This way no extra
 * disposables need to be managed when e.g. [map] is used.
 */
abstract class DependentVal<T>(
    private val dependencies: Iterable<Val<*>>,
) : AbstractVal<T>() {
    /**
     * Is either empty or has a disposable per dependency.
     */
    private val dependencyObservers = mutableListOf<Disposable>()

    protected var _value: T? = null

    override val value: T
        get() {
            if (hasNoObservers()) {
                _value = computeValue()
            }

            return _value.unsafeToNonNull()
        }

    override fun observe(callNow: Boolean, observer: ValObserver<T>): Disposable {
        if (hasNoObservers()) {
            dependencies.forEach { dependency ->
                dependencyObservers.add(
                    dependency.observe {
                        val oldValue = _value
                        _value = computeValue()

                        if (_value != oldValue) {
                            emit(oldValue.unsafeToNonNull())
                        }
                    }
                )
            }

            _value = computeValue()
        }

        val superDisposable = super.observe(callNow, observer)

        return disposable {
            superDisposable.dispose()

            if (observers.isEmpty()) {
                dependencyObservers.forEach { it.dispose() }
                dependencyObservers.clear()
            }
        }
    }

    protected fun hasObservers(): Boolean =
        dependencyObservers.isNotEmpty()

    protected fun hasNoObservers(): Boolean =
        dependencyObservers.isEmpty()

    protected abstract fun computeValue(): T
}
