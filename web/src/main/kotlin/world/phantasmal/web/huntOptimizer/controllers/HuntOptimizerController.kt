package world.phantasmal.web.huntOptimizer.controllers

import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.controllers.PathAwareTab
import world.phantasmal.web.core.controllers.PathAwareTabContainerController
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.huntOptimizer.HuntOptimizerUrls

class HuntOptimizerController(uiStore: UiStore) :
    PathAwareTabContainerController<PathAwareTab>(
        uiStore,
        PwToolType.HuntOptimizer,
        listOf(
            PathAwareTab("Optimize", HuntOptimizerUrls.optimize),
            PathAwareTab("Methods", HuntOptimizerUrls.methods),
            PathAwareTab("Help", HuntOptimizerUrls.help)
        )
    )
