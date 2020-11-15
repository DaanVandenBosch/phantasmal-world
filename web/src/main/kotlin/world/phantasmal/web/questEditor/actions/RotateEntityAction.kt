package world.phantasmal.web.questEditor.actions

import world.phantasmal.web.core.actions.Action
import world.phantasmal.web.externals.babylon.Vector3
import world.phantasmal.web.questEditor.models.QuestEntityModel

class RotateEntityAction(
    private val setSelectedEntity: (QuestEntityModel<*, *>) -> Unit,
    private val entity: QuestEntityModel<*, *>,
    private val newRotation: Vector3,
    private val oldRotation: Vector3,
    private val world: Boolean,
) : Action {
    override val description: String = "Rotate ${entity.type.simpleName}"

    override fun execute() {
        setSelectedEntity(entity)

        if (world) {
            entity.setWorldRotation(newRotation)
        } else {
            entity.setRotation(newRotation)
        }
    }

    override fun undo() {
        setSelectedEntity(entity)

        if (world) {
            entity.setWorldRotation(oldRotation)
        } else {
            entity.setRotation(oldRotation)
        }
    }
}
