package world.phantasmal.web.questEditor.actions

import world.phantasmal.observable.change
import world.phantasmal.web.core.actions.Action
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.QuestEntityPropModel

class EditEntityPropAction(
    private val setSelectedEntity: (QuestEntityModel<*, *>) -> Unit,
    private val entity: QuestEntityModel<*, *>,
    private val prop: QuestEntityPropModel,
    private val newValue: Any,
    private val oldValue: Any,
) : Action {
    override val description: String = "Edit ${entity.type.simpleName} ${prop.name}"

    override fun execute() {
        change {
            setSelectedEntity(entity)
            prop.setValue(newValue)
        }
    }

    override fun undo() {
        change {
            setSelectedEntity(entity)
            prop.setValue(oldValue)
        }
    }
}
