package world.phantasmal.web.huntOptimizer

import world.phantasmal.web.core.PwTool
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.huntOptimizer.controllers.HuntOptimizerController
import world.phantasmal.web.huntOptimizer.controllers.MethodsController
import world.phantasmal.web.huntOptimizer.controllers.MethodsForEpisodeController
import world.phantasmal.web.huntOptimizer.persistence.HuntMethodPersister
import world.phantasmal.web.huntOptimizer.stores.HuntMethodStore
import world.phantasmal.web.huntOptimizer.widgets.HuntOptimizerWidget
import world.phantasmal.web.huntOptimizer.widgets.MethodsForEpisodeWidget
import world.phantasmal.web.huntOptimizer.widgets.MethodsWidget
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.widgets.Widget

class HuntOptimizer(
    private val assetLoader: AssetLoader,
    private val uiStore: UiStore,
) : DisposableContainer(), PwTool {
    override val toolType = PwToolType.HuntOptimizer

    override fun initialize(): Widget {
        // Persistence
        val huntMethodPersister = HuntMethodPersister()

        // Stores
        val huntMethodStore =
            addDisposable(HuntMethodStore(uiStore, assetLoader, huntMethodPersister))

        // Controllers
        val huntOptimizerController = addDisposable(HuntOptimizerController(uiStore))
        val methodsController = addDisposable(MethodsController(uiStore))

        // Main Widget
        return HuntOptimizerWidget(
            ctrl = huntOptimizerController,
            createMethodsWidget = {
                MethodsWidget(methodsController) { episode ->
                    MethodsForEpisodeWidget(MethodsForEpisodeController(huntMethodStore, episode))
                }
            }
        )
    }
}
