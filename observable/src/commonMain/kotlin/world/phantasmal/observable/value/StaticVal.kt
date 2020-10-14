package world.phantasmal.observable.value

import world.phantasmal.core.disposable.Scope
import world.phantasmal.observable.Observer

class StaticVal<T>(override val value: T) : Val<T> {
    override fun observe(scope: Scope, callNow: Boolean, observer: ValObserver<T>) {
        if (callNow) {
            observer(ValChangeEvent(value, value))
        }
    }

    override fun observe(scope: Scope, observer: Observer<T>) {
        // Do nothing.
    }
}
