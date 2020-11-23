package world.phantasmal.web.viewer.controller

import world.phantasmal.webui.controllers.Tab
import world.phantasmal.webui.controllers.TabController

sealed class ViewerTab(override val title: String) : Tab {
    object Mesh : ViewerTab("Model")
    object Texture : ViewerTab("Texture")
}

class ViewerController : TabController<ViewerTab>(
    listOf(ViewerTab.Mesh, ViewerTab.Texture)
)
