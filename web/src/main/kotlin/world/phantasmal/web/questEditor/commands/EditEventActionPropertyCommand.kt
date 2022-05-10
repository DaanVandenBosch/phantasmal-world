package world.phantasmal.web.questEditor.commands

import world.phantasmal.web.core.commands.Command
import world.phantasmal.web.questEditor.models.QuestEventActionModel
import world.phantasmal.web.questEditor.models.QuestEventModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore

class EditEventActionPropertyCommand<EventAction : QuestEventActionModel, T>(
    private val questEditorStore: QuestEditorStore,
    override val description: String,
    private val event: QuestEventModel,
    private val action: EventAction,
    private val setter: (EventAction, T) -> Unit,
    private val newValue: T,
    private val oldValue: T,
) : Command {
    override fun execute() {
        questEditorStore.setEventActionProperty(event, action, setter, newValue)
    }

    override fun undo() {
        questEditorStore.setEventActionProperty(event, action, setter, oldValue)
    }
}
