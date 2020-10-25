package world.phantasmal.web.application.controllers

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.observable.value.Val
import world.phantasmal.web.core.stores.PwTool
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.webui.controllers.Controller

class NavigationController(
    scope: CoroutineScope,
    private val uiStore: UiStore,
) : Controller(scope) {
    val tools: Map<PwTool, Val<Boolean>> = uiStore.toolToActive

    fun setCurrentTool(tool: PwTool) {
        uiStore.setCurrentTool(tool)
    }
}
