package world.phantasmal.observable

import world.phantasmal.core.disposable.Disposable

class SimpleEmitter<T> : AbstractDependency(), Emitter<T> {
    override fun emit(event: ChangeEvent<T>) {
        for (dependent in dependents) {
            dependent.dependencyMightChange()
        }

        for (dependent in dependents) {
            dependent.dependencyChanged(this, event)
        }
    }

    override fun observe(observer: Observer<T>): Disposable =
        CallbackObserver(this, observer)
}
