package world.phantasmal.lib.fileFormats.quest

/**
 * Represents an entity type-specific property for accessing ambiguous parts of the entity data.
 */
class EntityProp(
    val name: String,
    val offset: Int,
    val type: EntityPropType,
)

enum class EntityPropType {
    I32,
    F32,

    /**
     * Signed 32-bit integer that represents an angle. 0x10000 is 360°.
     */
    Angle,
}
