package world.phantasmal.web.questEditor.models

import world.phantasmal.lib.fileFormats.ninja.angleToRad
import world.phantasmal.lib.fileFormats.ninja.radToAngle
import world.phantasmal.lib.fileFormats.quest.EntityProp
import world.phantasmal.lib.fileFormats.quest.EntityPropType
import world.phantasmal.lib.fileFormats.quest.ObjectType
import world.phantasmal.observable.value.MutableVal
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal

class QuestEntityPropModel(private val entity: QuestEntityModel<*, *>, prop: EntityProp) {
    private val _value: MutableVal<Any> = mutableVal(when (prop.type) {
        EntityPropType.I32 -> entity.entity.data.getInt(prop.offset)
        EntityPropType.F32 -> entity.entity.data.getFloat(prop.offset)
        EntityPropType.Angle -> angleToRad(entity.entity.data.getInt(prop.offset))
    })
    private val affectsModel: Boolean =
        when (entity.type) {
            ObjectType.Probe ->
                prop.offset == 40

            ObjectType.Saw,
            ObjectType.LaserDetect,
            -> prop.offset == 48

            ObjectType.Sonic,
            ObjectType.LittleCryotube,
            ObjectType.Cactus,
            ObjectType.BigBrownRock,
            ObjectType.BigBlackRocks,
            ObjectType.BeeHive,
            -> prop.offset == 52

            ObjectType.ForestConsole ->
                prop.offset == 56

            ObjectType.PrincipalWarp,
            ObjectType.LaserFence,
            ObjectType.LaserSquareFence,
            ObjectType.LaserFenceEx,
            ObjectType.LaserSquareFenceEx,
            -> prop.offset == 60

            else -> false
        }

    val name: String = prop.name
    val offset = prop.offset
    val type: EntityPropType = prop.type
    val value: Val<Any> = _value

    fun setValue(value: Any, propagateToEntity: Boolean = true) {
        when (type) {
            EntityPropType.I32 -> {
                require(value is Int)
                entity.entity.data.setInt(offset, value)
            }
            EntityPropType.F32 -> {
                require(value is Float)
                entity.entity.data.setFloat(offset, value)
            }
            EntityPropType.Angle -> {
                require(value is Float)
                entity.entity.data.setInt(offset, radToAngle(value))
            }
        }

        _value.value = value

        if (propagateToEntity && affectsModel) {
            (entity as QuestObjectModel).setModel(
                entity.entity.data.getInt(offset),
                propagateToProps = false,
            )
        }
    }
}
