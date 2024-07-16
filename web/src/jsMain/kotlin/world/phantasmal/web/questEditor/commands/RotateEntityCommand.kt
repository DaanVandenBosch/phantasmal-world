package world.phantasmal.web.questEditor.commands

import world.phantasmal.web.core.commands.Command
import world.phantasmal.web.externals.three.Euler
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore

class RotateEntityCommand(
    private val questEditorStore: QuestEditorStore,
    private val entity: QuestEntityModel<*, *>,
    private val newRotation: Euler,
    private val oldRotation: Euler,
    private val world: Boolean,
) : Command {
    override val description: String = "Rotate ${entity.type.simpleName}"

    override fun execute() {
        if (world) {
            questEditorStore.setEntityWorldRotation(entity, newRotation)
        } else {
            questEditorStore.setEntityRotation(entity, newRotation)
        }
    }

    override fun undo() {
        if (world) {
            questEditorStore.setEntityWorldRotation(entity, oldRotation)
        } else {
            questEditorStore.setEntityRotation(entity, oldRotation)
        }
    }
}
