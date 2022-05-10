package world.phantasmal.observable.cell.list

import world.phantasmal.core.unsafe.unsafeAssertNotNull

abstract class AbstractElementsWrappingListCell<E> : AbstractListCell<E>() {
    /**
     * When [value] is accessed and this property is null, a new wrapper is created that points to
     * [elements]. Before changes to [elements] are made, if there's a wrapper, the current
     * wrapper's backing list is set to a copy of [elements] and this property is set to null. This
     * way, accessing [value] acts like accessing a snapshot without making an actual copy
     * everytime. This is necessary because the contract is that a cell's new value is always != to
     * its old value whenever a change event was emitted (TODO: is this still the contract?).
     */
    // TODO: Optimize this by using a weak reference to avoid copying when nothing references the
    //       wrapper.
    // TODO: Just remove this because it's a huge headache? Does it matter that events are
    //       immutable?
    private var _elementsWrapper: DelegatingList<E>? = null
    protected val elementsWrapper: DelegatingList<E>
        get() {
            if (_elementsWrapper == null) {
                _elementsWrapper = DelegatingList(elements)
            }

            return unsafeAssertNotNull(_elementsWrapper)
        }

    protected abstract val elements: List<E>

    protected fun copyAndResetWrapper() {
        _elementsWrapper?.backingList = elements.toList()
        _elementsWrapper = null
    }
}
