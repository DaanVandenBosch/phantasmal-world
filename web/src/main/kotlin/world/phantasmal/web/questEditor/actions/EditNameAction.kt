package world.phantasmal.web.questEditor.actions

import world.phantasmal.web.core.actions.Action
import world.phantasmal.web.questEditor.models.QuestModel

class EditNameAction(
    private val quest: QuestModel,
    private val newName: String,
    private val oldName: String,
) : Action {
    override val description: String = "Edit name"

    override fun execute() {
        quest.setName(newName)
    }

    override fun undo() {
        quest.setName(oldName)
    }
}
