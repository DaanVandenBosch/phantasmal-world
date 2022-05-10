package world.phantasmal.web.questEditor.commands

import world.phantasmal.web.core.commands.Command
import world.phantasmal.web.questEditor.models.QuestEventModel
import world.phantasmal.web.questEditor.models.QuestModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore

class DeleteEventCommand(
    private val questEditorStore: QuestEditorStore,
    private val quest: QuestModel,
    private val index: Int,
    private val event: QuestEventModel,
) : Command {
    override val description: String = "Delete event ${event.id.value}"

    override fun execute() {
        questEditorStore.removeEvent(quest, event)
    }

    override fun undo() {
        questEditorStore.addEvent(quest, index, event)
    }
}
