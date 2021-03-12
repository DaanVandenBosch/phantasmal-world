package world.phantasmal.web.shared.dto

import kotlinx.serialization.Serializable
import world.phantasmal.lib.Episode
import world.phantasmal.lib.fileFormats.quest.NpcType

@Serializable
class EnemyDrop(
    val difficulty: Difficulty,
    val episode: Episode,
    val sectionId: SectionId,
    val enemy: NpcType,
    val itemTypeId: Int,
    val dropRate: Double,
    val rareRate: Double,
)

@Serializable
class BoxDrop(
    val difficulty: Difficulty,
    val episode: Episode,
    val sectionId: SectionId,
    val areaId: Int,
    val itemTypeId: Int,
    val dropRate: Double,
)
