package world.phantasmal.web.huntoptimizer.controllers

import world.phantasmal.lib.fileformats.quest.Episode
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.MutableListVal
import world.phantasmal.observable.value.list.mutableListVal
import world.phantasmal.web.core.controllers.PathAwareTab
import world.phantasmal.web.core.controllers.PathAwareTabController
import world.phantasmal.web.core.stores.PwTool
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.huntoptimizer.HuntOptimizerUrls
import world.phantasmal.web.huntoptimizer.models.HuntMethodModel
import world.phantasmal.web.huntoptimizer.stores.HuntMethodStore

class MethodsTab(title: String, path: String, val episode: Episode) : PathAwareTab(title, path)

class MethodsController(
    uiStore: UiStore,
    huntMethodStore: HuntMethodStore,
) : PathAwareTabController<MethodsTab>(
    uiStore,
    PwTool.HuntOptimizer,
    listOf(
        MethodsTab("Episode I", HuntOptimizerUrls.methodsEpisodeI, Episode.I),
        MethodsTab("Episode II", HuntOptimizerUrls.methodsEpisodeII, Episode.II),
        MethodsTab("Episode IV", HuntOptimizerUrls.methodsEpisodeIV, Episode.IV),
    )
) {
    private val _episodeToMethods = mutableMapOf<Episode, MutableListVal<HuntMethodModel>>()

    val episodeToMethods: Map<Episode, ListVal<HuntMethodModel>> = _episodeToMethods

    init {
        // TODO: Use filtered ListVals.
        addDisposable(huntMethodStore.methods.observe(callNow = true) { (methods) ->
            val ep1 = _episodeToMethods.getOrPut(Episode.I) { mutableListVal() }
            val ep2 = _episodeToMethods.getOrPut(Episode.II) { mutableListVal() }
            val ep4 = _episodeToMethods.getOrPut(Episode.IV) { mutableListVal() }

            ep1.clear()
            ep2.clear()
            ep4.clear()

            methods.forEach { method ->
                when (method.episode) {
                    Episode.I -> ep1.add(method)
                    Episode.II -> ep2.add(method)
                    Episode.IV -> ep4.add(method)
                }
            }
        })
    }
}
