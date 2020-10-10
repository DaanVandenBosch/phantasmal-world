package world.phantasmal.web.huntoptimizer.controllers

import world.phantasmal.web.core.controllers.PathAwareTab
import world.phantasmal.web.core.controllers.PathAwareTabController
import world.phantasmal.web.core.stores.PwTool
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.huntoptimizer.HuntOptimizerUrls

class HuntOptimizerController(uiStore: UiStore) : PathAwareTabController<PathAwareTab>(
    uiStore,
    PwTool.HuntOptimizer,
    listOf(
        PathAwareTab("Optimize", HuntOptimizerUrls.optimize),
        PathAwareTab("Methods", HuntOptimizerUrls.methods),
        PathAwareTab("Help", HuntOptimizerUrls.help)
    )
)
