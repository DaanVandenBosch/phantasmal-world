package world.phantasmal.web.questEditor.actions

import world.phantasmal.web.core.actions.Action
import world.phantasmal.web.questEditor.models.QuestModel

class EditIdAction(
    private val quest: QuestModel,
    private val newId: Int,
    private val oldId: Int,
) : Action {
    override val description: String = "Edit ID"

    override fun execute() {
        quest.setId(newId)
    }

    override fun undo() {
        quest.setId(oldId)
    }
}
