package world.phantasmal.web.questEditor.rendering

import world.phantasmal.observable.value.Val
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
    init {
        updateMatrix()

        addDisposables(
            entity.worldPosition.observe { updateMatrix() },
            entity.worldRotation.observe { updateMatrix() },
        )

        val isVisible: Val<Boolean>

        if (entity is QuestNpcModel) {
            isVisible =
                entity.sectionInitialized.map(
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
        instanceHelper.position.set(
            entity.worldPosition.value.x,
            entity.worldPosition.value.y,
            entity.worldPosition.value.z,
        )
        instanceHelper.rotation.set(
            entity.worldRotation.value.x,
            entity.worldRotation.value.y,
            entity.worldRotation.value.z,
        )
        instanceHelper.updateMatrix()
        mesh.setMatrixAt(instanceIndex, instanceHelper.matrix)
        mesh.instanceMatrix.needsUpdate = true
    }

    companion object {
        private val instanceHelper = Object3D()
    }
}
