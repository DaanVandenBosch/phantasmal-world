package world.phantasmal.web.questEditor.models

import world.phantasmal.lib.fileFormats.quest.EntityType
import world.phantasmal.lib.fileFormats.quest.QuestEntity
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.web.core.rendering.conversion.vec3ToBabylon
import world.phantasmal.web.externals.babylon.Vector3

abstract class QuestEntityModel<Type : EntityType, Entity : QuestEntity<Type>>(
    private val entity: Entity,
) {
    private val _position = mutableVal(vec3ToBabylon(entity.position))
    private val _worldPosition = mutableVal(_position.value)

    val type: Type get() = entity.type

    /**
     * Section-relative position
     */
    val position: Val<Vector3> = _position

    /**
     * World position
     */
    val worldPosition: Val<Vector3> = _worldPosition
}
