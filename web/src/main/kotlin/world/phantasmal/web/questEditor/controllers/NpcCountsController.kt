package world.phantasmal.web.questEditor.controllers

import world.phantasmal.lib.fileFormats.quest.NpcType
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.list.emptyListVal
import world.phantasmal.web.questEditor.models.QuestNpcModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.controllers.Controller

class NpcCountsController(store: QuestEditorStore) : Controller() {
    val unavailable: Val<Boolean> = store.currentQuest.map { it == null }

    val npcCounts: Val<List<NameWithCount>> = store.currentQuest
        .flatMap { it?.npcs ?: emptyListVal() }
        .map(::countNpcs)

    private fun countNpcs(npcs: List<QuestNpcModel>): List<NameWithCount> {
        val npcCounts = mutableMapOf<NpcType, Int>()
        var extraCanadines = 0

        for (npc in npcs) {
            // Don't count Vol Opt twice.
            if (npc.type != NpcType.VolOptPart2) {
                npcCounts[npc.type] = (npcCounts[npc.type] ?: 0) + 1

                // Cananes always come with 8 canadines.
                if (npc.type == NpcType.Canane) {
                    extraCanadines += 8
                }
            }
        }

        return npcCounts.entries
            // Sort by canonical order.
            .sortedBy { (npcType) -> npcType.ordinal }
            .map { (npcType, count) ->
                val extra = if (npcType == NpcType.Canadine) extraCanadines else 0
                NameWithCount(npcType.simpleName, (count + extra).toString())
            }
    }

    data class NameWithCount(val name: String, val count: String)
}
