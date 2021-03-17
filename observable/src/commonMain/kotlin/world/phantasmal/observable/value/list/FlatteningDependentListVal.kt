package world.phantasmal.observable.value.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.unsafeAssertNotNull
import world.phantasmal.observable.value.Val

/**
 * Similar to [DependentListVal], except that this val's [computeElements] returns a ListVal.
 */
class FlatteningDependentListVal<E>(
    dependencies: List<Val<*>>,
    private val computeElements: () -> ListVal<E>,
) : AbstractDependentListVal<E>(dependencies) {
    private var computedVal: ListVal<E>? = null
    private var computedValObserver: Disposable? = null

    override val elements: List<E> get() = computedVal.unsafeAssertNotNull().value

    override fun computeElements() {
        computedVal = computeElements.invoke()

        computedValObserver?.dispose()

        computedValObserver =
            if (hasObservers) {
                computedVal.unsafeAssertNotNull().observeList(observer = ::finalizeUpdate)
            } else {
                null
            }
    }

    override fun lastObserverRemoved() {
        super.lastObserverRemoved()

        computedValObserver?.dispose()
        computedValObserver = null
    }
}
