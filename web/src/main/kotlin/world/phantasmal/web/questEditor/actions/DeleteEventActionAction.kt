package world.phantasmal.web.questEditor.actions

import world.phantasmal.web.core.actions.Action
import world.phantasmal.web.questEditor.models.QuestEventActionModel
import world.phantasmal.web.questEditor.models.QuestEventModel

/**
 * Deletes a quest event action.
 */
class DeleteEventActionAction(
    private val setSelectedEvent: (QuestEventModel) -> Unit,
    private val event: QuestEventModel,
    private val index: Int,
    private val action: QuestEventActionModel,
) : Action {
    override val description: String =
        "Remove ${action.shortName} action from event ${event.id.value}"

    override fun execute() {
        setSelectedEvent(event)
        event.removeAction(action)
    }

    override fun undo() {
        setSelectedEvent(event)
        event.addAction(index, action)
    }
}
