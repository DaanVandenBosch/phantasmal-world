package world.phantasmal.web.huntOptimizer.controllers

import world.phantasmal.lib.fileFormats.quest.Episode
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.controllers.PathAwareTab
import world.phantasmal.web.core.controllers.PathAwareTabController
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.huntOptimizer.HuntOptimizerUrls

class MethodsTab(title: String, path: String, val episode: Episode) : PathAwareTab(title, path)

class MethodsController(uiStore: UiStore) : PathAwareTabController<MethodsTab>(
    uiStore,
    PwToolType.HuntOptimizer,
    listOf(
        MethodsTab("Episode I", HuntOptimizerUrls.methodsEpisodeI, Episode.I),
        MethodsTab("Episode II", HuntOptimizerUrls.methodsEpisodeII, Episode.II),
        MethodsTab("Episode IV", HuntOptimizerUrls.methodsEpisodeIV, Episode.IV),
    )
)
