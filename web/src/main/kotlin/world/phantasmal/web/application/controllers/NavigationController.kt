package world.phantasmal.web.application.controllers

import world.phantasmal.observable.value.Val
import world.phantasmal.web.core.stores.PwTool
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.webui.controllers.Controller

class NavigationController(private val uiStore: UiStore) : Controller() {
    val tools: Map<PwTool, Val<Boolean>> = uiStore.toolToActive

    fun setCurrentTool(tool: PwTool) {
        uiStore.setCurrentTool(tool)
    }
}
