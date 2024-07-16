package world.phantasmal.webui.stores

import kotlinx.coroutines.Dispatchers
import world.phantasmal.core.disposable.DisposableSupervisedScope
import world.phantasmal.webui.DisposableContainer

abstract class Store : DisposableContainer() {
    protected val scope = addDisposable(DisposableSupervisedScope(this::class, Dispatchers.Main))
}
