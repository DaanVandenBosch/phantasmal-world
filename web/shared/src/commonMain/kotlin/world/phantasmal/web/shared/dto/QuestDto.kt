package world.phantasmal.web.shared.dto

import kotlinx.serialization.Serializable

@Serializable
class QuestDto(
    val id: Int,
    val name: String,
    val episode: Int,
    val enemy_counts: Map<String, Int>,
)
