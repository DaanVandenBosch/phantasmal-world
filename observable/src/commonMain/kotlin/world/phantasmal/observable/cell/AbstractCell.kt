package world.phantasmal.observable.cell

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.observable.AbstractDependency
import world.phantasmal.observable.CallbackObserver
import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Observer

abstract class AbstractCell<T> : AbstractDependency(), Cell<T> {
    private var mightChangeEmitted = false

    final override fun observe(observer: Observer<T>): Disposable =
        observe(callNow = false, observer)

    override fun observe(callNow: Boolean, observer: Observer<T>): Disposable {
        val observingCell = CallbackObserver(this, observer)

        if (callNow) {
            observer(ChangeEvent(value))
        }

        return observingCell
    }

    protected fun emitMightChange() {
        if (!mightChangeEmitted) {
            mightChangeEmitted = true

            for (dependent in dependents) {
                dependent.dependencyMightChange()
            }
        }
    }

    protected fun emitDependencyChanged(event: ChangeEvent<*>?) {
        if (mightChangeEmitted) {
            mightChangeEmitted = false

            for (dependent in dependents) {
                dependent.dependencyChanged(this, event)
            }
        }
    }
}
