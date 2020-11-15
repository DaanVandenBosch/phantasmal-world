package world.phantasmal.web.questEditor.actions

import world.phantasmal.web.core.actions.Action
import world.phantasmal.web.questEditor.models.QuestModel

class EditLongDescriptionAction(
    private val quest: QuestModel,
    private val newLongDescription: String,
    private val oldLongDescription: String,
) : Action {
    override val description: String = "Edit long description"

    override fun execute() {
        quest.setLongDescription(newLongDescription)
    }

    override fun undo() {
        quest.setLongDescription(oldLongDescription)
    }
}
