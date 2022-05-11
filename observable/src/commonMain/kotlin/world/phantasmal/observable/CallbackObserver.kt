package world.phantasmal.observable

import world.phantasmal.core.disposable.TrackedDisposable

/**
 * Calls [callback] when one or more observable in [dependencies] changes.
 */
class CallbackObserver(
    private vararg val dependencies: Observable<*>,
    private val callback: () -> Unit,
) : TrackedDisposable(), Dependent, LeafDependent {

    init {
        for (dependency in dependencies) {
            dependency.addDependent(this)
        }
    }

    override fun dispose() {
        for (dependency in dependencies) {
            dependency.removeDependent(this)
        }

        super.dispose()
    }

    override fun dependencyInvalidated(dependency: Dependency<*>) {
        MutationManager.invalidated(this)
    }

    override fun pull() {
        var changed = false

        // We loop through all dependencies to ensure they're valid again.
        for (dependency in dependencies) {
            if (dependency.changeEvent != null) {
                changed = true
            }
        }

        if (changed) {
            callback()
        }
    }
}
