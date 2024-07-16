package world.phantasmal.web.questEditor.commands

import world.phantasmal.web.core.commands.Command
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.SectionModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore

class EditEntitySectionCommand(
    private val questEditorStore: QuestEditorStore,
    private val entity: QuestEntityModel<*, *>,
    private val newSectionId: Int,
    private val newSection: SectionModel?,
    private val oldSectionId: Int,
    private val oldSection: SectionModel?,
) : Command {
    override val description: String = "Edit ${entity.type.simpleName} section"

    init {
        require(newSection == null || newSectionId == newSection.id)
        require(oldSection == null || oldSectionId == oldSection.id)
    }

    override fun execute() {
        if (newSection != null) {
            questEditorStore.setEntitySection(entity, newSection)
        } else {
            questEditorStore.setEntitySectionId(entity, newSectionId)
        }
    }

    override fun undo() {
        if (oldSection != null) {
            questEditorStore.setEntitySection(entity, oldSection)
        } else {
            questEditorStore.setEntitySectionId(entity, oldSectionId)
        }
    }
}
