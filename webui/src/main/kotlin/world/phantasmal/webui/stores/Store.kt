package world.phantasmal.webui.stores

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.core.disposable.Scope
import world.phantasmal.core.disposable.TrackedDisposable

abstract class Store(scope: Scope) : TrackedDisposable(scope.scope()), CoroutineScope by scope {
    override fun internalDispose() {
        // Do nothing.
    }
}
