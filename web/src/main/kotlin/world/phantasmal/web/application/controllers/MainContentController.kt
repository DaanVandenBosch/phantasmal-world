package world.phantasmal.web.application.controllers

import world.phantasmal.observable.cell.Cell
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.webui.controllers.Controller

class MainContentController(uiStore: UiStore) : Controller() {
    val tools: Map<PwToolType, Cell<Boolean>> = uiStore.toolToActive
}
