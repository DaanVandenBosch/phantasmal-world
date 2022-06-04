package world.phantasmal.web.questEditor.controllers

import world.phantasmal.psolib.fileFormats.quest.NpcType
import world.phantasmal.cell.Cell
import world.phantasmal.cell.flatMap
import world.phantasmal.cell.isNull
import world.phantasmal.cell.list.emptyListCell
import world.phantasmal.cell.map
import world.phantasmal.web.questEditor.models.QuestNpcModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.controllers.Controller

class NpcCountsController(private val store: QuestEditorStore) : Controller() {
    val unavailable: Cell<Boolean> = store.currentQuest.isNull()

    val npcCounts: Cell<List<NameWithCount>> = store.currentQuest
        .flatMap { it?.npcs ?: emptyListCell() }
        .map(::countNpcs)

    fun focused() {
        store.makeMainUndoCurrent()
    }

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
