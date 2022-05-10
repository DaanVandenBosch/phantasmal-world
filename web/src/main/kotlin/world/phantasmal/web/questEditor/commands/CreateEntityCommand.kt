package world.phantasmal.web.questEditor.commands

import world.phantasmal.web.core.commands.Command
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.QuestModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore

class CreateEntityCommand(
    private val questEditorStore: QuestEditorStore,
    private val quest: QuestModel,
    private val entity: QuestEntityModel<*, *>,
) : Command {
    override val description: String = "Add ${entity.type.name}"

    override fun execute() {
        questEditorStore.addEntity(quest, entity)
    }

    override fun undo() {
        questEditorStore.removeEntity(quest, entity)
    }
}
