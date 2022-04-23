package world.phantasmal.observable.cell

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.observable.AbstractDependency
import world.phantasmal.observable.CallbackChangeObserver
import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.ChangeObserver

abstract class AbstractCell<T> : AbstractDependency(), Cell<T> {
    private var mightChangeEmitted = false

    override fun observeChange(observer: ChangeObserver<T>): Disposable =
        CallbackChangeObserver(this, observer)

    protected fun emitMightChange() {
        if (!mightChangeEmitted) {
            mightChangeEmitted = true

            for (dependent in dependents) {
                dependent.dependencyMightChange()
            }
        }
    }

    protected fun emitDependencyChangedEvent(event: ChangeEvent<*>?) {
        if (mightChangeEmitted) {
            mightChangeEmitted = false

            for (dependent in dependents) {
                dependent.dependencyChanged(this, event)
            }
        }
    }

    override fun toString(): String = "${this::class.simpleName}[$value]"
}
