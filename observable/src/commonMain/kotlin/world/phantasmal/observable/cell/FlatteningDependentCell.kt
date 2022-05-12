package world.phantasmal.observable.cell

import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Observable

/**
 * Similar to [DependentCell], except that this cell's [compute] returns a cell.
 */
class FlatteningDependentCell<T>(
    vararg dependencies: Observable<*>,
    compute: () -> Cell<T>,
) : AbstractFlatteningDependentCell<T, Cell<T>, ChangeEvent<T>>(dependencies, compute) {

    override fun createEvent(oldValue: T?, newValue: T): ChangeEvent<T> =
        ChangeEvent(newValue)
}
