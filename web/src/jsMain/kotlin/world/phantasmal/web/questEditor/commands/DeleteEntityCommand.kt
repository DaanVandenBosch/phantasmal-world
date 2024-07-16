package world.phantasmal.web.questEditor.commands

import world.phantasmal.web.core.commands.Command
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.QuestModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore

class DeleteEntityCommand(
    private val questEditorStore: QuestEditorStore,
    private val quest: QuestModel,
    private val entity: QuestEntityModel<*, *>,
) : Command {
    override val description: String = "Delete ${entity.type.name}"

    override fun execute() {
        questEditorStore.removeEntity(quest, entity)
    }

    override fun undo() {
        questEditorStore.addEntity(quest, entity)
    }
}
