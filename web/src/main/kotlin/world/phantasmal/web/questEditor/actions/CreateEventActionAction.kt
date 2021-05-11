package world.phantasmal.web.questEditor.actions

import world.phantasmal.web.core.actions.Action
import world.phantasmal.web.questEditor.models.QuestEventActionModel
import world.phantasmal.web.questEditor.models.QuestEventModel

/**
 * Creates a quest event action.
 */
class CreateEventActionAction(
    private val setSelectedEvent: (QuestEventModel) -> Unit,
    private val event: QuestEventModel,
    private val action: QuestEventActionModel,
) : Action {
    override val description: String =
        "Add ${action.shortName} action to event ${event.id.value}"

    override fun execute() {
        event.addAction(action)
        setSelectedEvent(event)
    }

    override fun undo() {
        event.removeAction(action)
        setSelectedEvent(event)
    }
}
