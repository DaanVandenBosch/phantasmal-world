package world.phantasmal.web.questEditor.rendering

import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.web.core.rendering.disposeObject3DResources
import world.phantasmal.web.externals.three.InstancedMesh
import world.phantasmal.web.questEditor.models.QuestEntityModel

/**
 * Contains instances of an InstancedMesh related to a quest entity.
 */
abstract class InstanceContainer<Entity : QuestEntityModel<*, *>, Inst : Instance<Entity>>(
    val mesh: InstancedMesh,
) : TrackedDisposable() {

    private val instances: MutableList<Inst> = mutableListOf()

    init {
        @Suppress("LeakingThis")
        mesh.userData = this
    }

    override fun dispose() {
        disposeObject3DResources(mesh)
        super.dispose()
    }

    fun getInstance(entity: Entity): Inst? =
        instances.find { it.entity == entity }

    fun getInstanceAt(instanceIndex: Int): Inst =
        instances[instanceIndex]

    fun addInstance(entity: Entity): Inst {
        val instanceIndex = mesh.count
        mesh.count++

        val instance = createInstance(entity, instanceIndex)
        instances.add(instance)
        return instance
    }

    fun removeInstance(entity: Entity) {
        val index = instances.indexOfFirst { it.entity == entity }

        if (index != -1) {
            removeAt(index)
        }
    }

    protected fun removeAt(index: Int) {
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

    protected abstract fun createInstance(entity: Entity, index: Int): Inst
}
