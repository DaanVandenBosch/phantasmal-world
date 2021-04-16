package world.phantasmal.observable.value

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.core.unsafeAssertNotNull
import world.phantasmal.observable.Observer

/**
 * Starts observing its dependencies when the first observer on this val is registered. Stops
 * observing its dependencies when the last observer on this val is disposed. This way no extra
 * disposables need to be managed when e.g. [map] is used.
 */
abstract class AbstractDependentVal<T>(
    private vararg val dependencies: Val<*>,
) : AbstractVal<T>() {
    /**
     * Is either empty or has a disposable per dependency.
     */
    private val dependencyObservers = mutableListOf<Disposable>()

    /**
     * Set to true right before actual observers are added.
     */
    protected var hasObservers = false

    protected var _value: T? = null

    override val value: T
        get() {
            if (!hasObservers) {
                _value = computeValue()
            }

            return _value.unsafeAssertNotNull()
        }

    override fun observe(callNow: Boolean, observer: Observer<T>): Disposable {
        if (dependencyObservers.isEmpty()) {
            hasObservers = true

            dependencies.forEach { dependency ->
                dependencyObservers.add(
                    dependency.observe {
                        val oldValue = _value
                        _value = computeValue()

                        if (_value != oldValue) {
                            emit()
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
                hasObservers = false
                dependencyObservers.forEach { it.dispose() }
                dependencyObservers.clear()
            }
        }
    }

    protected abstract fun computeValue(): T
}
