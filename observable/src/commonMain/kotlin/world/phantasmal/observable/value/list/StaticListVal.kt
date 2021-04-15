package world.phantasmal.observable.value.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.stubDisposable
import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Observer
import world.phantasmal.observable.value.*

class StaticListVal<E>(private val elements: List<E>) : ListVal<E> {
    private val firstOrNull = StaticVal(elements.firstOrNull())

    override val size: Val<Int> = value(elements.size)
    override val empty: Val<Boolean> = if (elements.isEmpty()) trueVal() else falseVal()
    override val notEmpty: Val<Boolean> = if (elements.isNotEmpty()) trueVal() else falseVal()

    override val value: List<E> = elements

    override fun get(index: Int): E =
        elements[index]

    override fun observe(callNow: Boolean, observer: Observer<List<E>>): Disposable {
        if (callNow) {
            observer(ChangeEvent(value))
        }

        return stubDisposable()
    }

    override fun observe(observer: Observer<List<E>>): Disposable = stubDisposable()

    override fun observeList(callNow: Boolean, observer: ListValObserver<E>): Disposable {
        if (callNow) {
            observer(ListChangeEvent.Change(0, emptyList(), value))
        }

        return stubDisposable()
    }

    override fun firstOrNull(): Val<E?> = firstOrNull
}
