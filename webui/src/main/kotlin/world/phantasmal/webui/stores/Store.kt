package world.phantasmal.webui.stores

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.core.disposable.Scope
import world.phantasmal.core.disposable.TrackedDisposable
import kotlin.coroutines.CoroutineContext

abstract class Store(
    scope: Scope,
    crScope: CoroutineScope,
) : TrackedDisposable(scope.scope()), CoroutineScope {
    override val coroutineContext: CoroutineContext = crScope.coroutineContext

    override fun internalDispose() {
        // Do nothing.
    }
}
