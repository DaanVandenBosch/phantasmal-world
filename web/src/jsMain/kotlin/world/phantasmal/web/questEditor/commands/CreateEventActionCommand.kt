package world.phantasmal.web.questEditor.commands

import world.phantasmal.web.core.commands.Command
import world.phantasmal.web.questEditor.models.QuestEventActionModel
import world.phantasmal.web.questEditor.models.QuestEventModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore

/**
 * Creates a quest event action.
 */
class CreateEventActionCommand(
    private val questEditorStore: QuestEditorStore,
    private val event: QuestEventModel,
    private val action: QuestEventActionModel,
) : Command {
    override val description: String =
        "Add ${action.shortName} action to event ${event.id.value}"

    override fun execute() {
        questEditorStore.addEventAction(event, action)
    }

    override fun undo() {
        questEditorStore.removeEventAction(event, action)
    }
}
