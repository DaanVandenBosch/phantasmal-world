package world.phantasmal.web.questEditor.rendering

import world.phantasmal.web.externals.three.InstancedMesh
import world.phantasmal.web.externals.three.Object3D
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.QuestObjectModel

class EntityInstance(
    entity: QuestEntityModel<*, *>,
    mesh: InstancedMesh,
    instanceIndex: Int,
    modelChanged: (instanceIndex: Int) -> Unit,
) : Instance<QuestEntityModel<*, *>>(entity, mesh, instanceIndex) {
    init {
        if (entity is QuestObjectModel) {
            addDisposable(entity.model.observe(callNow = false) {
                modelChanged(this.instanceIndex)
            })
        }

        addDisposables(
            entity.worldPosition.observe { updateMatrix() },
            entity.worldRotation.observe { updateMatrix() },
        )
    }

    override fun updateObjectMatrix(obj: Object3D) {
        obj.position.copy(entity.worldPosition.value)
        obj.rotation.copy(entity.worldRotation.value)
        obj.updateMatrix()
    }
}
