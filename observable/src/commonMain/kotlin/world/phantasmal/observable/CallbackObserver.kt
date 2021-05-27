package world.phantasmal.observable

import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.core.unsafe.unsafeCast

class CallbackObserver<T, E : ChangeEvent<T>>(
    private val dependency: Dependency,
    private val callback: (E) -> Unit,
) : TrackedDisposable(), Dependent {
    init {
        dependency.addDependent(this)
    }

    override fun dispose() {
        dependency.removeDependent(this)
        super.dispose()
    }

    override fun dependencyMightChange() {
        // Do nothing.
    }

    override fun dependencyChanged(dependency: Dependency, event: ChangeEvent<*>?) {
        if (event != null) {
            callback(unsafeCast(event))
        }
    }
}
