package world.phantasmal.lib.fileFormats.quest

interface EntityType {
    val name: String

    /**
     * Unique name. E.g. an episode II Delsaber would have (Ep. II) appended to its name.
     */
    val uniqueName: String

    /**
     * Name used in the game.
     * Might conflict with other NPC names (e.g. Delsaber from ep. I and ep. II).
     */
    val simpleName: String
}
