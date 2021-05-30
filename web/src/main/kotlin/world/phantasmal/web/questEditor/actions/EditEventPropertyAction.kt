package world.phantasmal.web.questEditor.actions

import world.phantasmal.observable.change
import world.phantasmal.web.core.actions.Action
import world.phantasmal.web.questEditor.models.QuestEventModel

class EditEventPropertyAction<T>(
    override val description: String,
    private val setSelectedEvent: (QuestEventModel) -> Unit,
    private val event: QuestEventModel,
    private val setter: (T) -> Unit,
    private val newValue: T,
    private val oldValue: T,
) : Action {
    override fun execute() {
        change {
            setSelectedEvent(event)
            setter(newValue)
        }
    }

    override fun undo() {
        change {
            setSelectedEvent(event)
            setter(oldValue)
        }
    }
}
