package world.phantasmal.observable.value.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.core.unsafeToNonNull
import world.phantasmal.observable.value.AbstractVal
import world.phantasmal.observable.value.ValObserver

class FoldedVal<T, R>(
    private val dependency: ListVal<T>,
    private val initial: R,
    private val operation: (R, T) -> R,
) : AbstractVal<R>() {
    private var dependencyDisposable: Disposable? = null
    private var internalValue: R? = null

    override val value: R
        get() {
            return if (dependencyDisposable == null) {
                computeValue()
            } else {
                internalValue.unsafeToNonNull()
            }
        }

    override fun observe(callNow: Boolean, observer: ValObserver<R>): Disposable {
        val superDisposable = super.observe(callNow, observer)

        if (dependencyDisposable == null) {
            internalValue = computeValue()

            dependencyDisposable = dependency.observe {
                val oldValue = internalValue
                internalValue = computeValue()
                emit(oldValue.unsafeToNonNull())
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
