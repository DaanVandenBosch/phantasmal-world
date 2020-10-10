package world.phantasmal.observable.value

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.observable.Observer

class StaticVal<T>(override val value: T) : Val<T> {
    override fun observe(callNow: Boolean, observer: ValObserver<T>): Disposable {
        if (callNow) {
            observer(ValChangeEvent(value, value))
        }

        return StaticValDisposable
    }

    override fun observe(observer: Observer<T>): Disposable = StaticValDisposable

    private object StaticValDisposable : Disposable {
        override fun dispose() {
            // Do nothing.
        }
    }
}
