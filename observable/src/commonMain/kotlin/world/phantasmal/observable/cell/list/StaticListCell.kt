package world.phantasmal.observable.cell.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.nopDisposable
import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Observer
import world.phantasmal.observable.cell.*

class StaticListCell<E>(private val elements: List<E>) : ListCell<E> {
    private val firstOrNull = StaticCell(elements.firstOrNull())

    override val size: Cell<Int> = cell(elements.size)
    override val empty: Cell<Boolean> = if (elements.isEmpty()) trueCell() else falseCell()
    override val notEmpty: Cell<Boolean> = if (elements.isNotEmpty()) trueCell() else falseCell()

    override val value: List<E> = elements

    override fun get(index: Int): E =
        elements[index]

    override fun observe(callNow: Boolean, observer: Observer<List<E>>): Disposable {
        if (callNow) {
            observer(ChangeEvent(value))
        }

        return nopDisposable()
    }

    override fun observe(observer: Observer<List<E>>): Disposable = nopDisposable()

    override fun observeList(callNow: Boolean, observer: ListObserver<E>): Disposable {
        if (callNow) {
            observer(ListChangeEvent.Change(0, emptyList(), value))
        }

        return nopDisposable()
    }

    override fun firstOrNull(): Cell<E?> = firstOrNull
}
