package world.phantasmal.web.questEditor.actions

import world.phantasmal.observable.change
import world.phantasmal.web.core.actions.Action
import world.phantasmal.web.questEditor.models.QuestEventModel
import world.phantasmal.web.questEditor.models.QuestModel

class CreateEventAction(
    private val setSelectedEvent: (QuestEventModel?) -> Unit,
    private val quest: QuestModel,
    private val index: Int,
    private val event: QuestEventModel,
) : Action {
    override val description: String = "Add event ${event.id.value}"

    override fun execute() {
        change {
            quest.addEvent(index, event)
            setSelectedEvent(event)
        }
    }

    override fun undo() {
        change {
            setSelectedEvent(null)
            quest.removeEvent(event)
        }
    }
}
