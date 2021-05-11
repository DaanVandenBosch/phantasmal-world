package world.phantasmal.observable.cell

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.core.unsafe.unsafeAssertNotNull
import world.phantasmal.observable.Observer

/**
 * Similar to [DependentCell], except that this cell's [compute] returns a cell.
 */
class FlatteningDependentCell<T>(
    vararg dependencies: Cell<*>,
    private val compute: () -> Cell<T>,
) : AbstractDependentCell<T>(*dependencies) {
    private var computedCell: Cell<T>? = null
    private var computedCellObserver: Disposable? = null

    override val value: T
        get() {
            return if (hasObservers) {
                computedCell.unsafeAssertNotNull().value
            } else {
                super.value
            }
        }

    override fun observe(callNow: Boolean, observer: Observer<T>): Disposable {
        val superDisposable = super.observe(callNow, observer)

        return disposable {
            superDisposable.dispose()

            if (!hasObservers) {
                computedCellObserver?.dispose()
                computedCellObserver = null
                computedCell = null
            }
        }
    }

    override fun computeValue(): T {
        val computedCell = compute()
        this.computedCell = computedCell

        computedCellObserver?.dispose()

        if (hasObservers) {
            computedCellObserver = computedCell.observe { (value) ->
                _value = value
                emit()
            }
        }

        return computedCell.value
    }
}
