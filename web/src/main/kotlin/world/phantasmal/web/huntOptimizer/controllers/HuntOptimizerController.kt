package world.phantasmal.web.huntOptimizer.controllers

import world.phantasmal.core.disposable.Scope
import world.phantasmal.web.core.controllers.PathAwareTab
import world.phantasmal.web.core.controllers.PathAwareTabController
import world.phantasmal.web.core.stores.PwTool
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.huntOptimizer.HuntOptimizerUrls

class HuntOptimizerController(scope: Scope, uiStore: UiStore) :
    PathAwareTabController<PathAwareTab>(
        scope,
        uiStore,
        PwTool.HuntOptimizer,
        listOf(
            PathAwareTab("Optimize", HuntOptimizerUrls.optimize),
            PathAwareTab("Methods", HuntOptimizerUrls.methods),
            PathAwareTab("Help", HuntOptimizerUrls.help)
        )
    )
