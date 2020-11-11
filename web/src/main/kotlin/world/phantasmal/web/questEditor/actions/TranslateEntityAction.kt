package world.phantasmal.web.questEditor.actions

import world.phantasmal.web.core.actions.Action
import world.phantasmal.web.externals.babylon.Vector3
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.SectionModel

class TranslateEntityAction(
    private val setSelectedEntity: (QuestEntityModel<*, *>) -> Unit,
    private val entity: QuestEntityModel<*, *>,
    private val oldSection: SectionModel?,
    private val newSection: SectionModel?,
    private val oldPosition: Vector3,
    private val newPosition: Vector3,
    private val world: Boolean,
) : Action {
    override val description: String = "Move ${entity.type.simpleName}"

    override fun execute() {
        setSelectedEntity(entity)

        newSection?.let(entity::setSection)

        if (world) {
            entity.setWorldPosition(newPosition)
        } else {
            entity.setPosition(newPosition)
        }
    }

    override fun undo() {
        setSelectedEntity(entity)

        oldSection?.let(entity::setSection)

        if (world) {
            entity.setWorldPosition(oldPosition)
        } else {
            entity.setPosition(oldPosition)
        }
    }
}
