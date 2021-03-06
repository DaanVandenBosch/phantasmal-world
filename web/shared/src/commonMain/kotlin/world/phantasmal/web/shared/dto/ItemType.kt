package world.phantasmal.web.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Instances of this class contain the data that is the same for every item of a specific type.
 * E.g. all spread needles are called "Spread Needle" and they all have the same base ATA.
 */
@Serializable
sealed class ItemType {
    abstract val id: Int
    abstract val name: String
}

@Serializable
@SerialName("weapon")
class WeaponItemType(
    override val id: Int,
    override val name: String,
    val minAtp: Int,
    val maxAtp: Int,
    val ata: Int,
    val maxGrind: Int,
    val requiredAtp: Int,
) : ItemType()

@Serializable
@SerialName("frame")
class FrameItemType(
    override val id: Int,
    override val name: String,
    val atp: Int,
    val ata: Int,
    val minEvp: Int,
    val maxEvp: Int,
    val minDfp: Int,
    val maxDfp: Int,
    val mst: Int,
    val hp: Int,
    val lck: Int,
) : ItemType()

@Serializable
@SerialName("barrier")
class BarrierItemType(
    override val id: Int,
    override val name: String,
    val atp: Int,
    val ata: Int,
    val minEvp: Int,
    val maxEvp: Int,
    val minDfp: Int,
    val maxDfp: Int,
    val mst: Int,
    val hp: Int,
    val lck: Int,
) : ItemType()

@Serializable
@SerialName("unit")
class UnitItemType(
    override val id: Int,
    override val name: String,
) : ItemType()

@Serializable
@SerialName("tool")
class ToolItemType(
    override val id: Int,
    override val name: String,
) : ItemType()
