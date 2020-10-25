package world.phantasmal.web.application.controllers

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.observable.value.Val
import world.phantasmal.web.core.stores.PwTool
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.webui.controllers.Controller

class MainContentController(scope: CoroutineScope, uiStore: UiStore) : Controller(scope) {
    val tools: Map<PwTool, Val<Boolean>> = uiStore.toolToActive
}
