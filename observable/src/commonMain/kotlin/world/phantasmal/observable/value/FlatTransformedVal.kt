package world.phantasmal.observable.value

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.core.unsafeToNonNull

class FlatTransformedVal<T>(
    dependencies: Iterable<Val<*>>,
    private val compute: () -> Val<T>,
) : DependentVal<T>(dependencies) {
    private var computedVal: Val<T>? = null
    private var computedValObserver: Disposable? = null

    override val value: T
        get() {
            return if (hasNoObservers()) {
                super.value
            } else {
                computedVal.unsafeToNonNull().value
            }
        }

    override fun observe(callNow: Boolean, observer: ValObserver<T>): Disposable {
        val superDisposable = super.observe(callNow, observer)

        return disposable {
            superDisposable.dispose()

            if (hasNoObservers()) {
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

        if (hasObservers()) {
            computedValObserver = computedVal.observe { (value) ->
                val oldValue = _value.unsafeToNonNull<T>()
                _value = value
                emit(oldValue)
            }
        }

        return computedVal.value
    }
}
