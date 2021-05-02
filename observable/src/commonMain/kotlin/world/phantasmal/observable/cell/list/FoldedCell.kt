package world.phantasmal.observable.cell.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.core.unsafe.unsafeCast
import world.phantasmal.observable.Observer
import world.phantasmal.observable.cell.AbstractCell

class FoldedCell<T, R>(
    private val dependency: ListCell<T>,
    private val initial: R,
    private val operation: (R, T) -> R,
) : AbstractCell<R>() {
    private var dependencyDisposable: Disposable? = null
    private var _value: R? = null

    override val value: R
        get() {
            return if (dependencyDisposable == null) {
                computeValue()
            } else {
                _value.unsafeCast()
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
