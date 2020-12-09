package world.phantasmal.web.viewer.controller

import world.phantasmal.webui.controllers.Tab
import world.phantasmal.webui.controllers.TabContainerController

sealed class ViewerTab(override val title: String) : Tab {
    object Mesh : ViewerTab("Model")
    object Texture : ViewerTab("Texture")
}

class ViewerController : TabContainerController<ViewerTab>(
    listOf(ViewerTab.Mesh, ViewerTab.Texture)
)
