package world.phantasmal.observable.cell.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.unsafe.unsafeAssertNotNull
import world.phantasmal.observable.CallbackChangeObserver
import world.phantasmal.observable.ChangeObserver
import world.phantasmal.observable.Observable
import world.phantasmal.observable.cell.AbstractFlatteningDependentCell
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.DependentCell

/**
 * Similar to [DependentListCell], except that this cell's computeElements returns a [ListCell].
 */
class FlatteningDependentListCell<E>(
    vararg dependencies: Observable<*>,
    computeElements: () -> ListCell<E>,
) :
    AbstractFlatteningDependentCell<List<E>, ListCell<E>, ListChangeEvent<E>>(
        dependencies,
        computeElements
    ),
    ListCell<E> {

    private var _size: Cell<Int>? = null
    override val size: Cell<Int>
        get() {
            if (_size == null) {
                _size = DependentCell(this) { value.size }
            }

            return unsafeAssertNotNull(_size)
        }

    private var _empty: Cell<Boolean>? = null
    override val empty: Cell<Boolean>
        get() {
            if (_empty == null) {
                _empty = DependentCell(this) { value.isEmpty() }
            }

            return unsafeAssertNotNull(_empty)
        }

    private var _notEmpty: Cell<Boolean>? = null
    override val notEmpty: Cell<Boolean>
        get() {
            if (_notEmpty == null) {
                _notEmpty = DependentCell(this) { value.isNotEmpty() }
            }

            return unsafeAssertNotNull(_notEmpty)
        }

    override fun observeChange(observer: ChangeObserver<List<E>>): Disposable =
        observeListChange(observer)

    override fun observeListChange(observer: ListChangeObserver<E>): Disposable =
        CallbackChangeObserver(this, observer)

    override fun toString(): String = listCellToString(this)

    override fun createEvent(oldValue: List<E>?, newValue: List<E>): ListChangeEvent<E> {
        val old = oldValue ?: emptyList()
        return ListChangeEvent(
            newValue,
            listOf(ListChange(
                index = 0,
                prevSize = old.size,
                removed = old,
                inserted = newValue,
            )),
        )
    }
}
