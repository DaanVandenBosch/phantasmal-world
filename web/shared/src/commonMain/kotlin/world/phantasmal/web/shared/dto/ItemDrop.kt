package world.phantasmal.web.shared.dto

import kotlinx.serialization.Serializable
import world.phantasmal.lib.Episode
import world.phantasmal.lib.fileFormats.quest.NpcType

@Serializable
sealed class ItemDrop {
    abstract val difficulty: Difficulty
    abstract val episode: Episode
    abstract val sectionId: SectionId
    abstract val itemTypeId: Int
    abstract val dropRate: Double
}

@Serializable
class EnemyDrop(
    override val difficulty: Difficulty,
    override val episode: Episode,
    override val sectionId: SectionId,
    val enemy: NpcType,
    override val itemTypeId: Int,
    /**
     * Chance that this enemy drops anything at all.
     */
    val anythingRate: Double,
    /**
     * Chance that an enemy drops this rare item, if it drops anything at all.
     */
    val rareRate: Double,
) : ItemDrop() {
    override val dropRate: Double = anythingRate * rareRate
}

@Serializable
class BoxDrop(
    override val difficulty: Difficulty,
    override val episode: Episode,
    override val sectionId: SectionId,
    val areaId: Int,
    override val itemTypeId: Int,
    override val dropRate: Double,
) : ItemDrop()
