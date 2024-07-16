package world.phantasmal.web.questEditor.rendering

import world.phantasmal.web.externals.three.InstancedMesh
import world.phantasmal.web.externals.three.Object3D
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.webui.DisposableContainer

/**
 * Represents an instance of an InstancedMesh related to a quest entity.
 */
abstract class Instance<Entity : QuestEntityModel<*, *>>(
    val entity: Entity,
    val mesh: InstancedMesh,
    var instanceIndex: Int,
) : DisposableContainer() {
    /**
     * When set, this object's transform will match the instance's transform.
     */
    var follower: Object3D? = null
        set(follower) {
            follower?.let(::updateObjectMatrix)
            field = follower
        }

    init {
        updateMatrix()
    }

    protected fun updateMatrix() {
        updateObjectMatrix(instanceHelper)
        mesh.setMatrixAt(instanceIndex, instanceHelper.matrix)
        mesh.instanceMatrix.needsUpdate = true

        follower?.let(::updateObjectMatrix)
    }

    protected abstract fun updateObjectMatrix(obj: Object3D)

    companion object {
        private val instanceHelper = Object3D()
    }
}
