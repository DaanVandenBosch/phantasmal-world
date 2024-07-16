package world.phantasmal.web.questEditor.commands

import world.phantasmal.web.core.commands.Command
import world.phantasmal.web.questEditor.models.QuestEventActionModel
import world.phantasmal.web.questEditor.models.QuestEventModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore

/**
 * Deletes a quest event action.
 */
class DeleteEventActionCommand(
    private val questEditorStore: QuestEditorStore,
    private val event: QuestEventModel,
    private val index: Int,
    private val action: QuestEventActionModel,
) : Command {
    override val description: String =
        "Remove ${action.shortName} action from event ${event.id.value}"

    override fun execute() {
        questEditorStore.removeEventAction(event, action)
    }

    override fun undo() {
        questEditorStore.addEventAction(event, index, action)
    }
}
