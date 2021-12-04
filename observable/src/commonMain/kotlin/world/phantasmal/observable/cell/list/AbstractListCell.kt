package world.phantasmal.observable.cell.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.unsafe.unsafeAssertNotNull
import world.phantasmal.observable.CallbackChangeObserver
import world.phantasmal.observable.ChangeObserver
import world.phantasmal.observable.cell.AbstractCell
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.DependentCell
import world.phantasmal.observable.cell.not

abstract class AbstractListCell<E> : AbstractCell<List<E>>(), ListCell<E> {
    /**
     * When [value] is accessed and this property is null, a new wrapper is created that points to
     * [elements]. Before changes to [elements] are made, if there's a wrapper, the current
     * wrapper's backing list is set to a copy of [elements] and this property is set to null. This
     * way, accessing [value] acts like accessing a snapshot without making an actual copy
     * everytime. This is necessary because the contract is that a cell's new value is always != to
     * its old value whenever a change event was emitted.
     */
    // TODO: Optimize this by using a weak reference to avoid copying when nothing references the
    //       wrapper.
    private var _elementsWrapper: DelegatingList<E>? = null
    protected val elementsWrapper: DelegatingList<E>
        get() {
            if (_elementsWrapper == null) {
                _elementsWrapper = DelegatingList(elements)
            }

            return unsafeAssertNotNull(_elementsWrapper)
        }

    protected abstract val elements: List<E>

    @Suppress("LeakingThis")
    final override val size: Cell<Int> = DependentCell(this) { value.size }

    final override val empty: Cell<Boolean> = DependentCell(size) { size.value == 0 }

    final override val notEmpty: Cell<Boolean> = !empty

    final override fun observeChange(observer: ChangeObserver<List<E>>): Disposable =
        observeListChange(observer)

    override fun observeListChange(observer: ListChangeObserver<E>): Disposable =
        CallbackChangeObserver(this, observer)

    protected fun copyAndResetWrapper() {
        _elementsWrapper?.backingList = elements.toList()
        _elementsWrapper = null
    }
}
