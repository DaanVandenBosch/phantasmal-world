package world.phantasmal.web.questEditor.actions

import world.phantasmal.web.core.actions.Action
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.SectionModel

class EditEntitySectionAction(
    private val entity: QuestEntityModel<*, *>,
    private val newSectionId: Int,
    private val newSection: SectionModel?,
    private val oldSectionId: Int,
    private val oldSection: SectionModel?,
) : Action {
    override val description: String = "Edit ${entity.type.simpleName} section"

    init {
        require(newSection == null || newSectionId == newSection.id)
        require(oldSection == null || oldSectionId == oldSection.id)
    }

    override fun execute() {
        if (newSection != null) {
            entity.setSection(newSection)
        } else {
            entity.setSectionId(newSectionId)
        }
    }

    override fun undo() {
        if (oldSection != null) {
            entity.setSection(oldSection)
        } else {
            entity.setSectionId(oldSectionId)
        }
    }
}
