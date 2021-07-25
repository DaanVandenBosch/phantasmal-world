// Use custom serializers because deserializing enums without @Serializable annotation is extremely
// slow. See https://github.com/Kotlin/kotlinx.serialization/issues/1385.
@file:UseSerializers(EpisodeSerializer::class, NpcTypeSerializer::class)

package world.phantasmal.web.shared.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.fileFormats.quest.NpcType

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

private object EpisodeSerializer : KSerializer<Episode> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(Episode::class.simpleName!!, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Episode) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): Episode =
        Episode.valueOf(decoder.decodeString())
}

private object NpcTypeSerializer : KSerializer<NpcType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(NpcType::class.simpleName!!, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: NpcType) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): NpcType =
        NpcType.valueOf(decoder.decodeString())
}

