package world.phantasmal.web.huntOptimizer.models

import world.phantasmal.lib.Episode
import world.phantasmal.lib.fileFormats.quest.NpcType

class SimpleQuestModel(
    val id: Int,
    val name: String,
    val episode: Episode,
    val enemyCounts: Map<NpcType, Int>,
)
