package world.phantasmal.web.questEditor.rendering

import world.phantasmal.web.externals.three.InstancedMesh
import world.phantasmal.web.externals.three.Object3D
import world.phantasmal.web.questEditor.models.QuestObjectModel

class DestinationInstance(
    entity: QuestObjectModel,
    mesh: InstancedMesh,
    instanceIndex: Int,
) : Instance<QuestObjectModel>(entity, mesh, instanceIndex) {
    init {
        addDisposables(
            entity.destinationPosition.observeChange { updateMatrix() },
            entity.destinationRotationY.observeChange { updateMatrix() },
        )
    }

    override fun updateObjectMatrix(obj: Object3D) {
        obj.position.copy(entity.destinationPosition.value)
        obj.rotation.set(.0, entity.destinationRotationY.value, .0)
        obj.updateMatrix()
    }
}
