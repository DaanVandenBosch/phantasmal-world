package world.phantasmal.web.core.models

/**
 * Instances of this interface contain the data that is the same for every item of a specific type.
 * E.g. all spread needles are called "Spread Needle" and they all have the same base ATA.
 */
interface ItemType {
    val id: Int
    val name: String
}
