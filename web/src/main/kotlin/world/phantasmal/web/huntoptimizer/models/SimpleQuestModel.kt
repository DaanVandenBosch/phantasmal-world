package world.phantasmal.web.huntoptimizer.models

import world.phantasmal.lib.fileformats.quest.Episode
import world.phantasmal.lib.fileformats.quest.NpcType

class SimpleQuestModel(
    val id: Int,
    val name: String,
    val episode: Episode,
    val enemyCounts: Map<NpcType, Int>,
)
