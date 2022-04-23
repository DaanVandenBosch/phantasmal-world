package world.phantasmal.observable.cell.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.unsafe.unsafeAssertNotNull
import world.phantasmal.observable.CallbackChangeObserver
import world.phantasmal.observable.ChangeObserver
import world.phantasmal.observable.Dependent
import world.phantasmal.observable.cell.AbstractDependentCell
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.DependentCell

abstract class AbstractDependentListCell<E> :
    AbstractDependentCell<List<E>>(),
    ListCell<E>,
    Dependent {

    protected abstract val elements: List<E>

    override val value: List<E>
        get() {
            if (dependents.isEmpty()) {
                computeElements()
            }

            return elements
        }

    private var _size: Cell<Int>? = null
    final override val size: Cell<Int>
        get() {
            if (_size == null) {
                _size = DependentCell(this) { value.size }
            }

            return unsafeAssertNotNull(_size)
        }

    private var _empty: Cell<Boolean>? = null
    final override val empty: Cell<Boolean>
        get() {
            if (_empty == null) {
                _empty = DependentCell(this) { value.isEmpty() }
            }

            return unsafeAssertNotNull(_empty)
        }

    private var _notEmpty: Cell<Boolean>? = null
    final override val notEmpty: Cell<Boolean>
        get() {
            if (_notEmpty == null) {
                _notEmpty = DependentCell(this) { value.isNotEmpty() }
            }

            return unsafeAssertNotNull(_notEmpty)
        }

    final override fun observeChange(observer: ChangeObserver<List<E>>): Disposable =
        observeListChange(observer)

    override fun observeListChange(observer: ListChangeObserver<E>): Disposable =
        CallbackChangeObserver(this, observer)

    final override fun dependenciesFinishedChanging() {
        val oldElements = value

        computeElements()

        emitDependencyChangedEvent(
            ListChangeEvent(
                elements,
                listOf(ListChange(
                    index = 0,
                    prevSize = oldElements.size,
                    removed = oldElements,
                    inserted = elements,
                )),
            )
        )
    }

    protected abstract fun computeElements()
}
