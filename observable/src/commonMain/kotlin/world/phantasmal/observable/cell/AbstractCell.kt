package world.phantasmal.observable.cell

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Observer

abstract class AbstractCell<T> : Cell<T> {
    protected val observers: MutableList<Observer<T>> = mutableListOf()

    final override fun observe(observer: Observer<T>): Disposable =
        observe(callNow = false, observer)

    override fun observe(callNow: Boolean, observer: Observer<T>): Disposable {
        observers.add(observer)

        if (callNow) {
            observer(ChangeEvent(value))
        }

        return disposable {
            observers.remove(observer)
        }
    }

    protected fun emit() {
        val event = ChangeEvent(value)
        observers.forEach { it(event) }
    }
}
