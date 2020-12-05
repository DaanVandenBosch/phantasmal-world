package world.phantasmal.web.questEditor.rendering

import world.phantasmal.observable.value.Val
import world.phantasmal.web.externals.three.InstancedMesh
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.WaveModel

/**
 * Represents a specific entity type and model combination. Contains a single [InstancedMesh] and
 * manages its instances.
 */
class EntityInstancedMesh(
    private val mesh: InstancedMesh,
    private val selectedWave: Val<WaveModel?>,
    /**
     * Called whenever an entity's model changes. At this point the entity's instance has already
     * been removed from this [EntityInstancedMesh]. The entity should then be added to the correct
     * [EntityInstancedMesh].
     */
    private val modelChanged: (QuestEntityModel<*, *>) -> Unit,
) {
    private val instances: MutableList<EntityInstance> = mutableListOf()

    init {
        mesh.userData = this
    }

    fun getInstance(entity: QuestEntityModel<*, *>): EntityInstance? =
        instances.find { it.entity == entity }

    fun getInstanceAt(instanceIndex: Int): EntityInstance =
        instances[instanceIndex]

    fun addInstance(entity: QuestEntityModel<*, *>): EntityInstance {
        val instanceIndex = mesh.count
        mesh.count++

        val instance = EntityInstance(
            entity,
            mesh,
            instanceIndex,
            selectedWave,
        ) { index ->
            removeAt(index)
            modelChanged(entity)
        }

        instances.add(instance)
        return instance
    }

    fun removeInstance(entity: QuestEntityModel<*, *>) {
        val index = instances.indexOfFirst { it.entity == entity }

        if (index != -1) {
            removeAt(index)
        }
    }

    private fun removeAt(index: Int) {
        val instance = instances.removeAt(index)
        mesh.count--

        for (i in index..instances.lastIndex) {
            mesh.instanceMatrix.copyAt(i, mesh.instanceMatrix, i + 1)
            instances[i].instanceIndex = i
        }

        mesh.instanceMatrix.needsUpdate = true
        instance.dispose()
    }

    fun clearInstances() {
        instances.forEach { it.dispose() }
        instances.clear()
        mesh.count = 0
    }
}
