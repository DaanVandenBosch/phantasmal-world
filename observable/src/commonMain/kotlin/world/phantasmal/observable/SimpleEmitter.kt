package world.phantasmal.observable

import world.phantasmal.core.disposable.Disposable

// TODO: Should multiple events be emitted somehow during a change set? At the moment no application
//       code seems to care.
class SimpleEmitter<T> : AbstractDependency<T>(), Emitter<T> {
    override var changeEvent: ChangeEvent<T>? = null
        private set

    override fun emit(event: ChangeEvent<T>) {
        applyChange {
            this.changeEvent = event
        }
    }

    override fun observeChange(observer: ChangeObserver<T>): Disposable =
        CallbackChangeObserver(this, observer)
}
