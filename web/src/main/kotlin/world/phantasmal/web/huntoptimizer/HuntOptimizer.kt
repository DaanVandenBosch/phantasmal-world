package world.phantasmal.web.huntoptimizer

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.core.disposable.DisposableContainer
import world.phantasmal.web.core.AssetLoader
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.huntoptimizer.controllers.HuntOptimizerController
import world.phantasmal.web.huntoptimizer.controllers.MethodsController
import world.phantasmal.web.huntoptimizer.stores.HuntMethodStore
import world.phantasmal.web.huntoptimizer.widgets.HuntOptimizerWidget
import world.phantasmal.web.huntoptimizer.widgets.MethodsWidget

class HuntOptimizer(
    scope: CoroutineScope,
    assetLoader: AssetLoader,
    uiStore: UiStore,
) : DisposableContainer() {
    val widget = HuntOptimizerWidget(addDisposable(HuntOptimizerController(uiStore))) {
        MethodsWidget(MethodsController(uiStore, HuntMethodStore(scope, uiStore, assetLoader)))
    }
}
