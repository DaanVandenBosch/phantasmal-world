package world.phantasmal.web.viewer

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.core.PwTool
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.rendering.DisposableThreeRenderer
import world.phantasmal.web.core.widgets.RendererWidget
import world.phantasmal.web.viewer.controller.ViewerController
import world.phantasmal.web.viewer.controller.ViewerToolbarController
import world.phantasmal.web.viewer.rendering.MeshRenderer
import world.phantasmal.web.viewer.rendering.TextureRenderer
import world.phantasmal.web.viewer.store.ViewerStore
import world.phantasmal.web.viewer.widgets.ViewerToolbar
import world.phantasmal.web.viewer.widgets.ViewerWidget
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.widgets.Widget

class Viewer(
    private val createThreeRenderer: (HTMLCanvasElement) -> DisposableThreeRenderer,
) : DisposableContainer(), PwTool {
    override val toolType = PwToolType.Viewer

    override fun initialize(): Widget {
        // Stores
        val viewerStore = addDisposable(ViewerStore())

        // Controllers
        val viewerController = addDisposable(ViewerController())
        val viewerToolbarController = addDisposable(ViewerToolbarController(viewerStore))

        // Rendering
        val meshRenderer = addDisposable(
            MeshRenderer(viewerStore, createThreeRenderer)
        )
        val textureRenderer = addDisposable(
            TextureRenderer(viewerStore, createThreeRenderer)
        )

        // Main Widget
        return ViewerWidget(
            viewerController,
            { ViewerToolbar(viewerToolbarController) },
            { RendererWidget(meshRenderer) },
            { RendererWidget(textureRenderer) },
        )
    }
}
