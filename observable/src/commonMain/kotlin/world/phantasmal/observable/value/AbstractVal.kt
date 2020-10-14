package world.phantasmal.observable.value

import world.phantasmal.core.disposable.Scope
import world.phantasmal.core.disposable.disposable
import world.phantasmal.observable.Observer

abstract class AbstractVal<T> : Val<T> {
    protected val observers: MutableList<ValObserver<T>> = mutableListOf()

    final override fun observe(scope: Scope, observer: Observer<T>) {
        observe(scope, callNow = false, observer)
    }

    override fun observe(scope: Scope, callNow: Boolean, observer: ValObserver<T>) {
        observers.add(observer)

        if (callNow) {
            observer(ValChangeEvent(value, value))
        }

        scope.disposable {
            observers.remove(observer)
        }
    }

    protected fun emit(oldValue: T) {
        val event = ValChangeEvent(value, oldValue)
        observers.forEach { it(event) }
    }
}
