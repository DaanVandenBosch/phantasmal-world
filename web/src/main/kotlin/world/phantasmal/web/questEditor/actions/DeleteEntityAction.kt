package world.phantasmal.web.questEditor.actions

import world.phantasmal.observable.change
import world.phantasmal.web.core.actions.Action
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.QuestModel

class DeleteEntityAction(
    private val setSelectedEntity: (QuestEntityModel<*, *>) -> Unit,
    private val quest: QuestModel,
    private val entity: QuestEntityModel<*, *>,
) : Action {
    override val description: String = "Delete ${entity.type.name}"

    override fun execute() {
        quest.removeEntity(entity)
    }

    override fun undo() {
        change {
            quest.addEntity(entity)
            setSelectedEntity(entity)
        }
    }
}
