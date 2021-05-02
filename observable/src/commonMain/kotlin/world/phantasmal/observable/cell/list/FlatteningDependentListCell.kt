package world.phantasmal.observable.cell.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.unsafe.unsafeAssertNotNull
import world.phantasmal.observable.cell.Cell

/**
 * Similar to [DependentListCell], except that this cell's [computeElements] returns a [ListCell].
 */
class FlatteningDependentListCell<E>(
    vararg dependencies: Cell<*>,
    private val computeElements: () -> ListCell<E>,
) : AbstractDependentListCell<E>(*dependencies) {
    private var computedCell: ListCell<E>? = null
    private var computedCellObserver: Disposable? = null

    override val elements: List<E> get() = computedCell.unsafeAssertNotNull().value

    override fun computeElements() {
        computedCell = computeElements.invoke()

        computedCellObserver?.dispose()

        computedCellObserver =
            if (hasObservers) {
                computedCell.unsafeAssertNotNull().observeList(observer = ::finalizeUpdate)
            } else {
                null
            }
    }

    override fun lastObserverRemoved() {
        super.lastObserverRemoved()

        computedCellObserver?.dispose()
        computedCellObserver = null
    }
}
