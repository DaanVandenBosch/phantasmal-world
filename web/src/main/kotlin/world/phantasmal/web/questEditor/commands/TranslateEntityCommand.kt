package world.phantasmal.web.questEditor.commands

import world.phantasmal.web.core.commands.Command
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore

class TranslateEntityCommand(
    private val questEditorStore: QuestEditorStore,
    private val entity: QuestEntityModel<*, *>,
    private val newSection: Int?,
    private val oldSection: Int?,
    private val newPosition: Vector3,
    private val oldPosition: Vector3,
    private val world: Boolean,
) : Command {
    override val description: String = "Move ${entity.type.simpleName}"

    override fun execute() {
        if (world) {
            questEditorStore.setEntityWorldPosition(entity, newSection, newPosition)
        } else {
            questEditorStore.setEntityPosition(entity, newSection, newPosition)
        }
    }

    override fun undo() {
        if (world) {
            questEditorStore.setEntityWorldPosition(entity, oldSection, oldPosition)
        } else {
            questEditorStore.setEntityPosition(entity, oldSection, oldPosition)
        }
    }
}
