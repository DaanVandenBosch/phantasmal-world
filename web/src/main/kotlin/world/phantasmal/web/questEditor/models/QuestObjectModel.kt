package world.phantasmal.web.questEditor.models

import world.phantasmal.psolib.fileFormats.quest.ObjectType
import world.phantasmal.psolib.fileFormats.quest.QuestObject
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.mutableCell
import world.phantasmal.web.core.rendering.conversion.vec3ToThree
import world.phantasmal.web.externals.three.Vector3

class QuestObjectModel(obj: QuestObject) : QuestEntityModel<ObjectType, QuestObject>(obj) {
    val hasDestination = obj.destinationPositionOffset != -1

    private val _model = mutableCell(if (obj.modelOffset == -1) null else obj.model)
    val model: Cell<Int?> = _model

    private val _destinationPosition = mutableCell(vec3ToThree(obj.destinationPosition))
    val destinationPosition: Cell<Vector3> = _destinationPosition

    private val _destinationRotationY = mutableCell(obj.destinationRotationY.toDouble())
    val destinationRotationY: Cell<Double> = _destinationRotationY

    fun setModel(model: Int, propagateToProps: Boolean = true) {
        entity.model = model
        _model.value = model

        if (propagateToProps) {
            propagateChangeToProperties(entity.modelOffset, 4)
        }
    }

    fun setDestinationPosition(pos: Vector3, propagateToProps: Boolean = true) {
        entity.setDestinationPosition(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())
        _destinationPosition.value = pos

        if (propagateToProps) {
            propagateChangeToProperties(entity.destinationPositionOffset, 12)
        }
    }

    fun setDestinationRotationY(rotY: Double, propagateToProps: Boolean = true) {
        entity.destinationRotationY = rotY.toFloat()
        _destinationRotationY.value = rotY

        if (propagateToProps) {
            propagateChangeToProperties(entity.destinationRotationYOffset, 4)
        }
    }

    private fun propagateChangeToProperties(offset: Int, size: Int) {
        for (prop in properties.value) {
            if (prop.overlaps(offset, size)) {
                prop.updateValue()
            }
        }
    }
}
