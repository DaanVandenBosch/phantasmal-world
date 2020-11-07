package world.phantasmal.web.viewer

import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.core.PwTool
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.externals.babylon.Engine
import world.phantasmal.web.viewer.controller.ViewerToolbarController
import world.phantasmal.web.viewer.rendering.MeshRenderer
import world.phantasmal.web.viewer.store.ViewerStore
import world.phantasmal.web.viewer.widgets.ViewerToolbar
import world.phantasmal.web.viewer.widgets.ViewerWidget
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.widgets.Widget

class Viewer(
    private val createEngine: (HTMLCanvasElement) -> Engine,
) : DisposableContainer(), PwTool {
    override val toolType = PwToolType.Viewer

    override fun initialize(scope: CoroutineScope): Widget {
        // Stores
        val viewerStore = addDisposable(ViewerStore(scope))

        // Controllers
        val viewerToolbarController = addDisposable(ViewerToolbarController(viewerStore))

        // Rendering
        val canvas = document.createElement("CANVAS") as HTMLCanvasElement
        val renderer = addDisposable(MeshRenderer(viewerStore, canvas, createEngine(canvas)))

        // Main Widget
        return ViewerWidget(scope, ViewerToolbar(scope, viewerToolbarController), canvas, renderer)
    }
}
