package world.phantasmal.lib.fileFormats.quest

/**
 * Represents a configurable property for accessing parts of entity data of which the use is not
 * fully understood or ambiguous.
 */
class EntityProp(
    val name: String,
    val offset: Int,
    val type: EntityPropType,
)

enum class EntityPropType {
    U8,
    U16,
    U32,
    I8,
    I16,
    I32,
    F32,

    /**
     * Signed 32-bit integer that represents an angle. 0x10000 is 360°.
     */
    Angle,
}
