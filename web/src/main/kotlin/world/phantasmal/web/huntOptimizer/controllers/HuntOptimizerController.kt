package world.phantasmal.web.huntOptimizer.controllers

import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.controllers.PathAwareTab
import world.phantasmal.web.core.controllers.PathAwareTabContainerController
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.huntOptimizer.HuntOptimizerUrls

sealed class HuntOptimizerTab(
    override val title: String,
    override val path: String,
) : PathAwareTab {
    object Optimize : HuntOptimizerTab("Optimize", HuntOptimizerUrls.optimize)
    object Methods : HuntOptimizerTab("Methods", HuntOptimizerUrls.methods)
    object Help : HuntOptimizerTab("Help", HuntOptimizerUrls.help)
}

class HuntOptimizerController(uiStore: UiStore) :
    PathAwareTabContainerController<HuntOptimizerTab>(
        uiStore,
        PwToolType.HuntOptimizer,
        tabs = listOf(
            HuntOptimizerTab.Optimize,
            HuntOptimizerTab.Methods,
            HuntOptimizerTab.Help,
        )
    )
