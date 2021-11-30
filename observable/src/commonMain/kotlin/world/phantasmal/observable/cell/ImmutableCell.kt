package world.phantasmal.observable.cell

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.nopDisposable
import world.phantasmal.observable.AbstractDependency
import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Observer

class ImmutableCell<T>(override val value: T) : AbstractDependency(), Cell<T> {
    override fun observe(callNow: Boolean, observer: Observer<T>): Disposable {
        if (callNow) {
            observer(ChangeEvent(value))
        }

        return nopDisposable()
    }

    override fun observe(observer: Observer<T>): Disposable = nopDisposable()

    override fun emitDependencyChanged() {
        error("StaticCell can't change.")
    }
}
