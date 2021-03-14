package world.phantasmal.observable.value

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.core.unsafeToNonNull
import world.phantasmal.observable.Observer

class FlatMappedVal<T>(
    dependencies: Iterable<Val<*>>,
    private val compute: () -> Val<T>,
) : AbstractDependentVal<T>(dependencies) {
    private var computedVal: Val<T>? = null
    private var computedValObserver: Disposable? = null

    override val value: T
        get() {
            return if (hasObservers) {
                computedVal.unsafeToNonNull().value
            } else {
                super.value
            }
        }

    override fun observe(callNow: Boolean, observer: Observer<T>): Disposable {
        val superDisposable = super.observe(callNow, observer)

        return disposable {
            superDisposable.dispose()

            if (!hasObservers) {
                computedValObserver?.dispose()
                computedValObserver = null
                computedVal = null
            }
        }
    }

    override fun computeValue(): T {
        val computedVal = compute()
        this.computedVal = computedVal

        computedValObserver?.dispose()

        if (hasObservers) {
            computedValObserver = computedVal.observe { (value) ->
                _value = value
                emit()
            }
        }

        return computedVal.value
    }
}
