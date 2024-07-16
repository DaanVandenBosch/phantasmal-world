package world.phantasmal.web.huntOptimizer.models

import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.fileFormats.quest.NpcType

class SimpleQuestModel(
    val id: Int,
    val name: String,
    val episode: Episode,
    val enemyCounts: Map<NpcType, Int>,
)
