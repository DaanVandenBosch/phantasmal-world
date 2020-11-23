package world.phantasmal.lib.fileFormats.quest

import world.phantasmal.lib.fileFormats.Vec3

interface QuestEntity<Type : EntityType> {
    val type: Type

    var areaId: Int

    var sectionId: Int

    /**
     * Section-relative position.
     */
    var position: Vec3

    var rotation: Vec3

    /**
     * Set the section-relative position.
     */
    fun setPosition(x: Float, y: Float, z: Float)

    fun setRotation(x: Float, y: Float, z: Float)
}
