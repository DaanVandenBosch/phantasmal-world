package world.phantasmal.observable.value

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.observable.Observer

abstract class AbstractVal<T> : Val<T> {
    protected val observers: MutableList<ValObserver<T>> = mutableListOf()

    final override fun observe(observer: Observer<T>): Disposable =
        observe(callNow = false, observer)

    override fun observe(callNow: Boolean, observer: ValObserver<T>): Disposable {
        observers.add(observer)

        if (callNow) {
            observer(ValChangeEvent(value, value))
        }

        return disposable {
            observers.remove(observer)
        }
    }

    protected fun emit(oldValue: T) {
        val event = ValChangeEvent(value, oldValue)
        observers.forEach { it(event) }
    }
}
