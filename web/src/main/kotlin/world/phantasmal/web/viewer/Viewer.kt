package world.phantasmal.web.viewer

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.externals.babylon.Engine
import world.phantasmal.web.viewer.controller.ViewerToolbarController
import world.phantasmal.web.viewer.rendering.MeshRenderer
import world.phantasmal.web.viewer.store.ViewerStore
import world.phantasmal.web.viewer.widgets.ViewerToolbar
import world.phantasmal.web.viewer.widgets.ViewerWidget
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.widgets.Widget

class Viewer(
    private val scope: CoroutineScope,
    private val createEngine: (HTMLCanvasElement) -> Engine,
) : DisposableContainer() {
    // Stores
    private val viewerStore = addDisposable(ViewerStore(scope))

    // Controllers
    private val viewerToolbarController = addDisposable(ViewerToolbarController(viewerStore))

    fun createWidget(): Widget =
        ViewerWidget(scope, ViewerToolbar(scope, viewerToolbarController), ::createViewerRenderer)

    private fun createViewerRenderer(canvas: HTMLCanvasElement): MeshRenderer =
        MeshRenderer(viewerStore, canvas, createEngine(canvas))
}
