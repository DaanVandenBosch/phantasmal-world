package world.phantasmal.web.questEditor.rendering

import world.phantasmal.web.externals.three.InstancedMesh
import world.phantasmal.web.questEditor.models.QuestEntityModel

/**
 * Represents a specific entity type and model combination. Contains a single [InstancedMesh] and
 * manages its instances. Takes ownership of the given mesh.
 */
class EntityInstanceContainer(
    mesh: InstancedMesh,
    /**
     * Called whenever an entity's model changes. At this point the entity's instance has already
     * been removed from this [EntityInstanceContainer]. The entity should then be added to the correct
     * [EntityInstanceContainer].
     */
    private val modelChanged: (QuestEntityModel<*, *>) -> Unit,
) : InstanceContainer<QuestEntityModel<*, *>, EntityInstance>(mesh) {
    override fun createInstance(entity: QuestEntityModel<*, *>, index: Int): EntityInstance =
        EntityInstance(entity, mesh, index) { idx ->
            removeAt(idx)
            modelChanged(entity)
        }
}
