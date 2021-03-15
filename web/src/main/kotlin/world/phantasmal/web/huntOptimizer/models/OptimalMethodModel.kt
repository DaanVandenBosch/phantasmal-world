package world.phantasmal.web.huntOptimizer.models

import world.phantasmal.lib.Episode
import world.phantasmal.web.shared.dto.Difficulty
import world.phantasmal.web.shared.dto.ItemType
import world.phantasmal.web.shared.dto.SectionId
import kotlin.time.Duration

class OptimalMethodModel(
    val difficulty: Difficulty,
    val sectionIds: List<SectionId>,
    val name: String,
    val episode: Episode,
    val methodTime: Duration,
    val runs: Double,
    val itemTypeIdToCount: Map<Int, Double>,
) {
    val totalTime: Duration = methodTime * runs
}
