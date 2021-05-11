package world.phantasmal.web.questEditor.actions

import world.phantasmal.web.core.actions.Action
import world.phantasmal.web.questEditor.models.QuestEventModel
import world.phantasmal.web.questEditor.models.QuestModel

class DeleteEventAction(
    private val setSelectedEvent: (QuestEventModel?) -> Unit,
    private val quest: QuestModel,
    private val index: Int,
    private val event: QuestEventModel,
) : Action {
    override val description: String = "Delete event ${event.id.value}"

    override fun execute() {
        setSelectedEvent(null)
        quest.removeEvent(event)
    }

    override fun undo() {
        quest.addEvent(index, event)
        setSelectedEvent(event)
    }
}
