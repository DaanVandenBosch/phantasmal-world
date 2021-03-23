package world.phantasmal.web.viewer

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.core.PwTool
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.rendering.DisposableThreeRenderer
import world.phantasmal.web.core.widgets.RendererWidget
import world.phantasmal.web.viewer.controllers.CharacterClassOptionsController
import world.phantasmal.web.viewer.controllers.ViewerController
import world.phantasmal.web.viewer.controllers.ViewerToolbarController
import world.phantasmal.web.viewer.loading.CharacterClassAssetLoader
import world.phantasmal.web.viewer.rendering.MeshRenderer
import world.phantasmal.web.viewer.rendering.TextureRenderer
import world.phantasmal.web.viewer.stores.ViewerStore
import world.phantasmal.web.viewer.widgets.CharacterClassOptionsWidget
import world.phantasmal.web.viewer.widgets.ViewerToolbar
import world.phantasmal.web.viewer.widgets.ViewerWidget
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.widgets.Widget

class Viewer(
    private val assetLoader: AssetLoader,
    private val createThreeRenderer: (HTMLCanvasElement) -> DisposableThreeRenderer,
) : DisposableContainer(), PwTool {
    override val toolType = PwToolType.Viewer

    override fun initialize(): Widget {
        // Asset Loaders
        val characterClassAssetLoader = addDisposable(CharacterClassAssetLoader(assetLoader))

        // Stores
        val viewerStore = addDisposable(ViewerStore(characterClassAssetLoader))

        // Controllers
        val viewerController = addDisposable(ViewerController(viewerStore))
        val viewerToolbarController = addDisposable(ViewerToolbarController(viewerStore))
        val characterClassOptionsController =
            addDisposable(CharacterClassOptionsController(viewerStore))

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
            { CharacterClassOptionsWidget(characterClassOptionsController) },
            { RendererWidget(meshRenderer) },
            { RendererWidget(textureRenderer) },
        )
    }
}
