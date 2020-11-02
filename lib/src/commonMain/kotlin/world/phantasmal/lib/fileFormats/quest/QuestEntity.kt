package world.phantasmal.lib.fileFormats.quest

import world.phantasmal.lib.fileFormats.Vec3

interface QuestEntity<Type : EntityType> {
    val type: Type

    /**
     * Section-relative position.
     */
    var position: Vec3

    var rotation: Vec3
}
