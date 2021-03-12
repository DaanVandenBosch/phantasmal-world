package world.phantasmal.web.huntOptimizer

import world.phantasmal.web.core.PwTool
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.stores.ItemDropStore
import world.phantasmal.web.core.stores.ItemTypeStore
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.huntOptimizer.controllers.HuntOptimizerController
import world.phantasmal.web.huntOptimizer.controllers.MethodsController
import world.phantasmal.web.huntOptimizer.controllers.MethodsForEpisodeController
import world.phantasmal.web.huntOptimizer.controllers.WantedItemsController
import world.phantasmal.web.huntOptimizer.persistence.HuntMethodPersister
import world.phantasmal.web.huntOptimizer.persistence.WantedItemPersister
import world.phantasmal.web.huntOptimizer.stores.HuntMethodStore
import world.phantasmal.web.huntOptimizer.stores.HuntOptimizerStore
import world.phantasmal.web.huntOptimizer.widgets.*
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.widgets.Widget

class HuntOptimizer(
    private val assetLoader: AssetLoader,
    private val uiStore: UiStore,
) : DisposableContainer(), PwTool {
    override val toolType = PwToolType.HuntOptimizer

    override fun initialize(): Widget {
        val itemTypeStore = addDisposable(ItemTypeStore(assetLoader))

        // Persistence
        val huntMethodPersister = HuntMethodPersister()
        val wantedItemPersister = WantedItemPersister(itemTypeStore)

        // Stores
        val huntMethodStore =
            addDisposable(HuntMethodStore(uiStore, assetLoader, huntMethodPersister))
        val itemDropStore = addDisposable(ItemDropStore(assetLoader))
        val huntOptimizerStore = addDisposable(HuntOptimizerStore(
            wantedItemPersister,
            uiStore,
            huntMethodStore,
            itemTypeStore,
            itemDropStore,
        ))

        // Controllers
        val huntOptimizerController = addDisposable(HuntOptimizerController(uiStore))
        val wantedItemsController = addDisposable(WantedItemsController(huntOptimizerStore))
        val methodsController = addDisposable(MethodsController(uiStore))

        // Main Widget
        return HuntOptimizerWidget(
            ctrl = huntOptimizerController,
            createOptimizerWidget = {
                OptimizerWidget(
                    { WantedItemsWidget(wantedItemsController) },
                    { OptimizationResultWidget() },
                )
            },
            createMethodsWidget = {
                MethodsWidget(methodsController) { episode ->
                    MethodsForEpisodeWidget(MethodsForEpisodeController(huntMethodStore, episode))
                }
            }
        )
    }
}
