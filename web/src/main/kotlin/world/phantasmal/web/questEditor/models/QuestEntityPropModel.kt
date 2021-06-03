package world.phantasmal.web.questEditor.models

import world.phantasmal.lib.fileFormats.ninja.angleToRad
import world.phantasmal.lib.fileFormats.ninja.radToAngle
import world.phantasmal.lib.fileFormats.quest.EntityProp
import world.phantasmal.lib.fileFormats.quest.EntityPropType
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.MutableCell
import world.phantasmal.observable.cell.mutableCell
import world.phantasmal.web.externals.three.Vector3

class QuestEntityPropModel(private val entity: QuestEntityModel<*, *>, prop: EntityProp) {
    private val _value: MutableCell<Any> = mutableCell(
        when (prop.type) {
            EntityPropType.I32 -> entity.entity.data.getInt(prop.offset)
            EntityPropType.F32 -> entity.entity.data.getFloat(prop.offset)
            EntityPropType.Angle -> angleToRad(entity.entity.data.getInt(prop.offset))
        }
    )
    private val affectsModel: Boolean
    private val affectsDestinationPosition: Boolean
    private val affectsDestinationRotationY: Boolean

    val name: String = prop.name
    val offset = prop.offset
    val type: EntityPropType = prop.type
    val value: Cell<Any> = _value

    init {
        affectsModel = entity is QuestObjectModel &&
                entity.entity.modelOffset != -1 &&
                overlaps(entity.entity.modelOffset, 4)

        affectsDestinationPosition = entity is QuestObjectModel &&
                entity.entity.destinationPositionOffset != -1 &&
                overlaps(entity.entity.destinationPositionOffset, 12)

        affectsDestinationRotationY = entity is QuestObjectModel &&
                entity.entity.destinationRotationYOffset != -1 &&
                overlaps(entity.entity.destinationRotationYOffset, 4)
    }

    fun setValue(value: Any) {
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

        if (affectsModel) {
            (entity as QuestObjectModel).setModel(
                entity.entity.model,
                propagateToProps = false,
            )
        }

        if (affectsDestinationPosition) {
            (entity as QuestObjectModel).setDestinationPosition(
                Vector3(
                    entity.entity.destinationPositionX.toDouble(),
                    entity.entity.destinationPositionY.toDouble(),
                    entity.entity.destinationPositionZ.toDouble(),
                ),
                propagateToProps = false,
            )
        }

        if (affectsDestinationRotationY) {
            (entity as QuestObjectModel).setDestinationRotationY(
                entity.entity.destinationRotationY.toDouble(),
                propagateToProps = false,
            )
        }
    }

    fun updateValue() {
        _value.value = when (type) {
            EntityPropType.I32 -> entity.entity.data.getInt(offset)
            EntityPropType.F32 -> entity.entity.data.getFloat(offset)
            EntityPropType.Angle -> angleToRad(entity.entity.data.getInt(offset))
        }
    }

    fun overlaps(offset: Int, size: Int): Boolean =
        this.offset < offset + size && this.offset + 4 > offset
}
