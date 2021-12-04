package world.phantasmal.observable

import world.phantasmal.core.disposable.TrackedDisposable

/**
 * Calls [callback] when one or more dependency in [dependencies] changes.
 */
class CallbackObserver(
    private vararg val dependencies: Dependency,
    private val callback: () -> Unit,
) : TrackedDisposable(), Dependent {
    private var changingDependencies = 0
    private var dependenciesActuallyChanged = false

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

    override fun dependencyMightChange() {
        changingDependencies++
    }

    override fun dependencyChanged(dependency: Dependency, event: ChangeEvent<*>?) {
        if (event != null) {
            dependenciesActuallyChanged = true
        }

        changingDependencies--

        if (changingDependencies == 0 && dependenciesActuallyChanged) {
            dependenciesActuallyChanged = false

            callback()
        }
    }
}
