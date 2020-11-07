package world.phantasmal.web.application.controllers

import world.phantasmal.observable.value.Val
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.webui.controllers.Controller

class MainContentController(uiStore: UiStore) : Controller() {
    val tools: Map<PwToolType, Val<Boolean>> = uiStore.toolToActive
}
