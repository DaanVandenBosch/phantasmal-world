package world.phantasmal.web.questEditor.commands

import world.phantasmal.web.core.commands.Command
import world.phantasmal.web.questEditor.models.QuestModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore

class EditQuestPropertyCommand<T>(
    private val questEditorStore: QuestEditorStore,
    override val description: String,
    private val quest: QuestModel,
    private val setter: (QuestModel, T) -> Unit,
    private val newValue: T,
    private val oldValue: T,
) : Command {
    override fun execute() {
        questEditorStore.setQuestProperty(quest, setter, newValue)
    }

    override fun undo() {
        questEditorStore.setQuestProperty(quest, setter, oldValue)
    }
}
