package world.phantasmal.web.huntOptimizer

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.core.disposable.DisposableContainer
import world.phantasmal.web.core.AssetLoader
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.huntOptimizer.controllers.HuntOptimizerController
import world.phantasmal.web.huntOptimizer.controllers.MethodsController
import world.phantasmal.web.huntOptimizer.stores.HuntMethodStore
import world.phantasmal.web.huntOptimizer.widgets.HuntOptimizerWidget
import world.phantasmal.web.huntOptimizer.widgets.MethodsWidget

class HuntOptimizer(
    scope: CoroutineScope,
    assetLoader: AssetLoader,
    uiStore: UiStore,
) : DisposableContainer() {
    val widget = HuntOptimizerWidget(addDisposable(HuntOptimizerController(uiStore))) {
        MethodsWidget(MethodsController(uiStore, HuntMethodStore(scope, uiStore, assetLoader)))
    }
}
