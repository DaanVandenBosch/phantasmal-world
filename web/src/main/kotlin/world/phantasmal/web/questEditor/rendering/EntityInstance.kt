package world.phantasmal.web.questEditor.rendering

import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.map
import world.phantasmal.web.externals.three.InstancedMesh
import world.phantasmal.web.externals.three.Object3D
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.QuestNpcModel
import world.phantasmal.web.questEditor.models.QuestObjectModel
import world.phantasmal.web.questEditor.models.WaveModel
import world.phantasmal.webui.DisposableContainer

class EntityInstance(
    val entity: QuestEntityModel<*, *>,
    val mesh: InstancedMesh,
    var instanceIndex: Int,
    selectedWave: Val<WaveModel?>,
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

        // TODO: Visibility.
        val isVisible: Val<Boolean>

        if (entity is QuestNpcModel) {
            isVisible =
                map(
                    entity.sectionInitialized,
                    selectedWave,
                    entity.wave
                ) { sectionInitialized, sWave, entityWave ->
                    sectionInitialized && (sWave == null || sWave == entityWave)
                }
        } else {
            isVisible = entity.section.isNotNull()

            if (entity is QuestObjectModel) {
                addDisposable(entity.model.observe(callNow = false) {
                    modelChanged(instanceIndex)
                })
            }
        }

//            observe(isVisible) { visible ->
//                mesh.setEnabled(visible)
//            }
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
