package world.phantasmal.web.huntOptimizer

import world.phantasmal.web.core.PwTool
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.stores.ItemDropStore
import world.phantasmal.web.core.stores.ItemTypeStore
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.huntOptimizer.controllers.*
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

        // Main Widget
        return HuntOptimizerWidget(
            ctrl = addDisposable(HuntOptimizerController(uiStore)),
            createOptimizerWidget = {
                OptimizerWidget(
                    createWantedItemsWidget = {
                        WantedItemsWidget(addDisposable(WantedItemsController(huntOptimizerStore)))
                    },
                    createOptimizationResultWidget = {
                        OptimizationResultWidget(
                            addDisposable(OptimizationResultController(huntOptimizerStore))
                        )
                    },
                )
            },
            createMethodsWidget = {
                MethodsWidget(addDisposable(MethodsController(uiStore))) { episode ->
                    MethodsForEpisodeWidget(MethodsForEpisodeController(huntMethodStore, episode))
                }
            }
        )
    }
}
