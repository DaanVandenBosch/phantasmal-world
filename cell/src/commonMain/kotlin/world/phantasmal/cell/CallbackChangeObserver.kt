package world.phantasmal.cell

import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.core.unsafe.unsafeCast

/**
 * Calls [callback] when [dependency] changes.
 */
class CallbackChangeObserver<T, E : ChangeEvent<T>>(
    private val dependency: Cell<T>,
    // We don't use ChangeObserver<T> because of type system limitations. It would break e.g.
    // AbstractListCell.observeListChange.
    private val callback: (E) -> Unit,
) : TrackedDisposable(), Dependent, LeafDependent {

    init {
        dependency.addDependent(this)
    }

    override fun dispose() {
        dependency.removeDependent(this)
        super.dispose()
    }

    override fun dependencyInvalidated(dependency: Dependency<*>) {
        MutationManager.invalidated(this)
    }

    override fun pull() {
        // See comment above callback property to understand why this is safe.
        dependency.changeEvent?.let(unsafeCast<(ChangeEvent<T>) -> Unit>(callback))
    }
}
