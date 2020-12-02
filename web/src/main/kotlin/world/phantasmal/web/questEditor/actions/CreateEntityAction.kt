package world.phantasmal.web.questEditor.actions

import world.phantasmal.web.core.actions.Action
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.QuestModel

class CreateEntityAction(
    private val setSelectedEntity: (QuestEntityModel<*, *>) -> Unit,
    private val quest: QuestModel,
    private val entity: QuestEntityModel<*, *>,
) : Action {
    override val description: String = "Create ${entity.type.name}"

    override fun execute() {
        quest.addEntity(entity)
        setSelectedEntity(entity)
    }

    override fun undo() {
        quest.removeEntity(entity)
    }
}
