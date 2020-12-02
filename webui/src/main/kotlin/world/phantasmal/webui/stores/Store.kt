package world.phantasmal.webui.stores

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import world.phantasmal.webui.DisposableContainer

abstract class Store : DisposableContainer() {
    protected val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    override fun internalDispose() {
        scope.cancel("Store disposed.")
        super.internalDispose()
    }
}
