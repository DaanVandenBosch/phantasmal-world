package world.phantasmal.web.huntOptimizer.controllers

import world.phantasmal.lib.fileFormats.quest.Episode
import world.phantasmal.lib.fileFormats.quest.NpcType
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.web.huntOptimizer.models.HuntMethodModel
import world.phantasmal.web.huntOptimizer.stores.HuntMethodStore
import world.phantasmal.webui.controllers.Controller

class MethodsForEpisodeController(
    huntMethodStore: HuntMethodStore,
    episode: Episode,
) : Controller() {
    val enemies: List<NpcType> = NpcType.VALUES.filter { it.enemy && it.episode == episode }

    val methods: ListVal<HuntMethodModel> =
        huntMethodStore.methods.filtered { it.episode == episode }
}
