package world.phantasmal.observable.value.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.stubDisposable
import world.phantasmal.observable.Observer
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.ValChangeEvent
import world.phantasmal.observable.value.ValObserver
import world.phantasmal.observable.value.value

class StaticListVal<E>(elements: List<E>) : ListVal<E> {
    override val sizeVal: Val<Int> = value(elements.size)

    override val value: List<E> = elements

    override fun observe(callNow: Boolean, observer: ValObserver<List<E>>): Disposable {
        if (callNow) {
            observer(ValChangeEvent(value, value))
        }

        return stubDisposable()
    }

    override fun observe(observer: Observer<List<E>>): Disposable = stubDisposable()

    override fun observeList(callNow: Boolean, observer: ListValObserver<E>): Disposable {
        if (callNow) {
            observer(ListValChangeEvent.Change(0, emptyList(), value))
        }

        return stubDisposable()
    }
}
