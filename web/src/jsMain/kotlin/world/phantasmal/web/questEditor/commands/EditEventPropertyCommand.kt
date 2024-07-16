package world.phantasmal.web.questEditor.commands

import world.phantasmal.web.core.commands.Command
import world.phantasmal.web.questEditor.models.QuestEventModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore

class EditEventPropertyCommand<T>(
    private val questEditorStore: QuestEditorStore,
    override val description: String,
    private val event: QuestEventModel,
    private val setter: (QuestEventModel, T) -> Unit,
    private val newValue: T,
    private val oldValue: T,
) : Command {
    override fun execute() {
        questEditorStore.setEventProperty(event, setter, newValue)
    }

    override fun undo() {
        questEditorStore.setEventProperty(event, setter, oldValue)
    }
}
