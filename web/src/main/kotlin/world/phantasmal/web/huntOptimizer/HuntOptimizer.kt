package world.phantasmal.web.huntOptimizer

import kotlinx.coroutines.CoroutineScope
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
    private val scope: CoroutineScope,
    assetLoader: AssetLoader,
    uiStore: UiStore,
) : DisposableContainer() {
    private val huntMethodStore = addDisposable(HuntMethodStore(scope, uiStore, assetLoader))

    private val huntOptimizerController = addDisposable(HuntOptimizerController(uiStore))
    private val methodsController =
        addDisposable(MethodsController(uiStore, huntMethodStore))

    fun createWidget(): Widget =
        HuntOptimizerWidget(
            scope,
            ctrl = huntOptimizerController,
            createMethodsWidget = { scope -> MethodsWidget(scope, methodsController) }
        )
}
