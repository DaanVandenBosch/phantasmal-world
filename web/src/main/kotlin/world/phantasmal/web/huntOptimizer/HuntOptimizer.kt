package world.phantasmal.web.huntOptimizer

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.core.disposable.Scope
import world.phantasmal.web.core.AssetLoader
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.huntOptimizer.controllers.HuntOptimizerController
import world.phantasmal.web.huntOptimizer.controllers.MethodsController
import world.phantasmal.web.huntOptimizer.stores.HuntMethodStore
import world.phantasmal.web.huntOptimizer.widgets.HuntOptimizerWidget
import world.phantasmal.web.huntOptimizer.widgets.MethodsWidget

class HuntOptimizer(
    scope: Scope,
    crScope: CoroutineScope,
    assetLoader: AssetLoader,
    uiStore: UiStore,
) {
    private val huntMethodStore = HuntMethodStore(scope, crScope, uiStore, assetLoader)

    private val huntOptimizerController = HuntOptimizerController(scope, uiStore)
    private val methodsController = MethodsController(scope, uiStore, huntMethodStore)

    val widget = HuntOptimizerWidget(
        scope,
        ctrl = huntOptimizerController,
        createMethodsWidget = { scope -> MethodsWidget(scope, methodsController) }
    )
}
