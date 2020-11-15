package world.phantasmal.web.questEditor.actions

import world.phantasmal.web.core.actions.Action
import world.phantasmal.web.questEditor.models.QuestModel

class EditShortDescriptionAction(
    private val quest: QuestModel,
    private val newShortDescription: String,
    private val oldShortDescription: String,
) : Action {
    override val description: String = "Edit short description"

    override fun execute() {
        quest.setShortDescription(newShortDescription)
    }

    override fun undo() {
        quest.setShortDescription(oldShortDescription)
    }
}
