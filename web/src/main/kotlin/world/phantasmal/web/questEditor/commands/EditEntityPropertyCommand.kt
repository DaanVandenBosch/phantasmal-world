package world.phantasmal.web.questEditor.commands

import world.phantasmal.web.core.commands.Command
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore

/**
 * Edits a simple entity property.
 */
class EditEntityPropertyCommand<Entity : QuestEntityModel<*, *>, T>(
    private val questEditorStore: QuestEditorStore,
    override val description: String,
    private val entity: Entity,
    private val setter: (Entity, T) -> Unit,
    private val newValue: T,
    private val oldValue: T,
) : Command {
    override fun execute() {
        questEditorStore.setEntityProperty(entity, setter, newValue)
    }

    override fun undo() {
        questEditorStore.setEntityProperty(entity, setter, oldValue)
    }
}
