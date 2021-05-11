package world.phantasmal.web.questEditor.rendering

import world.phantasmal.web.externals.three.InstancedMesh
import world.phantasmal.web.externals.three.Object3D
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.QuestObjectModel
import world.phantasmal.webui.DisposableContainer

class EntityInstance(
    val entity: QuestEntityModel<*, *>,
    val mesh: InstancedMesh,
    var instanceIndex: Int,
    modelChanged: (instanceIndex: Int) -> Unit,
) : DisposableContainer() {
    /**
     * When set, this object's transform will match the instance's transform.
     */
    var follower: Object3D? = null
        set(follower) {
            follower?.let {
                follower.position.copy(entity.worldPosition.value)
                follower.rotation.copy(entity.worldRotation.value)
                follower.updateMatrix()
            }

            field = follower
        }

    init {
        updateMatrix()

        addDisposables(
            entity.worldPosition.observe { updateMatrix() },
            entity.worldRotation.observe { updateMatrix() },
        )

        if (entity is QuestObjectModel) {
            addDisposable(entity.model.observe(callNow = false) {
                modelChanged(instanceIndex)
            })
        }
    }

    private fun updateMatrix() {
        val pos = entity.worldPosition.value
        val rot = entity.worldRotation.value
        instanceHelper.position.copy(pos)
        instanceHelper.rotation.copy(rot)
        instanceHelper.updateMatrix()
        mesh.setMatrixAt(instanceIndex, instanceHelper.matrix)
        mesh.instanceMatrix.needsUpdate = true

        follower?.let { follower ->
            follower.position.copy(pos)
            follower.rotation.copy(rot)
            follower.updateMatrix()
        }
    }

    companion object {
        private val instanceHelper = Object3D()
    }
}
