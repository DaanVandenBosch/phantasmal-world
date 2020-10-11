package world.phantasmal.webui.stores

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.core.disposable.DisposableContainer
import kotlin.coroutines.CoroutineContext

abstract class Store(scope: CoroutineScope) : DisposableContainer(), CoroutineScope {
    override val coroutineContext: CoroutineContext = scope.coroutineContext
}
