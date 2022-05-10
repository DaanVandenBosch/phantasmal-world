package world.phantasmal.web.questEditor.commands

import world.phantasmal.web.core.commands.Command
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.QuestEntityPropModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore

/**
 * Edits a dynamic entity property.
 */
class EditEntityPropCommand(
    private val questEditorStore: QuestEditorStore,
    private val entity: QuestEntityModel<*, *>,
    private val prop: QuestEntityPropModel,
    private val newValue: Any,
    private val oldValue: Any,
) : Command {
    override val description: String = "Edit ${entity.type.simpleName} ${prop.name}"

    override fun execute() {
        questEditorStore.setEntityProp(entity, prop, newValue)
    }

    override fun undo() {
        questEditorStore.setEntityProp(entity, prop, oldValue)
    }
}
