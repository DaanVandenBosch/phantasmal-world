package world.phantasmal.web.questEditor.actions

import world.phantasmal.observable.change
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
        change {
            event.addAction(action)
            setSelectedEvent(event)
        }
    }

    override fun undo() {
        change {
            event.removeAction(action)
            setSelectedEvent(event)
        }
    }
}
