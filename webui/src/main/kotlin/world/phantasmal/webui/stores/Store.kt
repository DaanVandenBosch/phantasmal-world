package world.phantasmal.webui.stores

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.webui.DisposableContainer

abstract class Store(protected val scope: CoroutineScope) :
    DisposableContainer(),
    CoroutineScope by scope
