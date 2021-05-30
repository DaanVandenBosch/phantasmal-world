package world.phantasmal.observable

import world.phantasmal.core.disposable.Disposable

class SimpleEmitter<T> : AbstractDependency(), Emitter<T> {
    private var event: ChangeEvent<T>? = null

    override fun emit(event: ChangeEvent<T>) {
        for (dependent in dependents) {
            dependent.dependencyMightChange()
        }

        this.event = event

        ChangeManager.changed(this)
    }

    override fun observe(observer: Observer<T>): Disposable =
        CallbackObserver(this, observer)

    override fun emitDependencyChanged() {
        if (event != null) {
            try {
                for (dependent in dependents) {
                    dependent.dependencyChanged(this, event)
                }
            } finally {
                event = null
            }
        }
    }
}
