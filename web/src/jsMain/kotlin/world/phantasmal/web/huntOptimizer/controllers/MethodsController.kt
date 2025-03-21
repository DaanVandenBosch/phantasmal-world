package world.phantasmal.web.huntOptimizer.controllers

import world.phantasmal.psolib.Episode
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.controllers.PathAwareTab
import world.phantasmal.web.core.controllers.PathAwareTabContainerController
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.huntOptimizer.HuntOptimizerUrls

class MethodsTab(
    override val title: String,
    override val path: String,
    val episode: Episode,
) : PathAwareTab

class MethodsController(uiStore: UiStore) : PathAwareTabContainerController<MethodsTab>(
    uiStore,
    PwToolType.HuntOptimizer,
    listOf(
        MethodsTab("Episode I", HuntOptimizerUrls.methodsEpisodeI, Episode.I),
        MethodsTab("Episode II", HuntOptimizerUrls.methodsEpisodeII, Episode.II),
        MethodsTab("Episode IV", HuntOptimizerUrls.methodsEpisodeIV, Episode.IV),
    )
)
