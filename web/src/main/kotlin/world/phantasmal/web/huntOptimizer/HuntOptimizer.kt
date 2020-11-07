package world.phantasmal.web.huntOptimizer

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.web.core.PwTool
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.huntOptimizer.controllers.HuntOptimizerController
import world.phantasmal.web.huntOptimizer.controllers.MethodsController
import world.phantasmal.web.huntOptimizer.stores.HuntMethodStore
import world.phantasmal.web.huntOptimizer.widgets.HuntOptimizerWidget
import world.phantasmal.web.huntOptimizer.widgets.MethodsWidget
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.widgets.Widget

class HuntOptimizer(
    private val assetLoader: AssetLoader,
    private val uiStore: UiStore,
) : DisposableContainer(), PwTool {
    override val toolType = PwToolType.HuntOptimizer

    override fun initialize(scope: CoroutineScope): Widget {
        // Stores
        val huntMethodStore = addDisposable(HuntMethodStore(scope, uiStore, assetLoader))

        // Controllers
        val huntOptimizerController = addDisposable(HuntOptimizerController(uiStore))
        val methodsController = addDisposable(MethodsController(uiStore, huntMethodStore))

        // Main Widget
        return HuntOptimizerWidget(
            scope,
            ctrl = huntOptimizerController,
            createMethodsWidget = { s -> MethodsWidget(s, methodsController) }
        )
    }
}
