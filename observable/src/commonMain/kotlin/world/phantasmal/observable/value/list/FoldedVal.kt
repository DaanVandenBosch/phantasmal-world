package world.phantasmal.observable.value.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.core.unsafe.unsafeAssertNotNull
import world.phantasmal.observable.Observer
import world.phantasmal.observable.value.AbstractVal

class FoldedVal<T, R>(
    private val dependency: ListVal<T>,
    private val initial: R,
    private val operation: (R, T) -> R,
) : AbstractVal<R>() {
    private var dependencyDisposable: Disposable? = null
    private var _value: R? = null

    override val value: R
        get() {
            return if (dependencyDisposable == null) {
                computeValue()
            } else {
                _value.unsafeAssertNotNull()
            }
        }

    override fun observe(callNow: Boolean, observer: Observer<R>): Disposable {
        val superDisposable = super.observe(callNow, observer)

        if (dependencyDisposable == null) {
            _value = computeValue()

            dependencyDisposable = dependency.observe {
                _value = computeValue()
                emit()
            }
        }

        return disposable {
            superDisposable.dispose()

            if (observers.isEmpty()) {
                dependencyDisposable?.dispose()
                dependencyDisposable = null
            }
        }
    }

    private fun computeValue(): R = dependency.value.fold(initial, operation)
}
