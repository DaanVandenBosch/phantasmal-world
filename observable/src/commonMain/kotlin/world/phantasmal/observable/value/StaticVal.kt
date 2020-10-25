package world.phantasmal.observable.value

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.stubDisposable
import world.phantasmal.observable.Observer

class StaticVal<T>(override val value: T) : Val<T> {
    override fun observe(callNow: Boolean, observer: ValObserver<T>): Disposable {
        if (callNow) {
            observer(ValChangeEvent(value, value))
        }

        return stubDisposable()
    }

    override fun observe(observer: Observer<T>): Disposable = stubDisposable()
}
