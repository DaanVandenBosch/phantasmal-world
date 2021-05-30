package world.phantasmal.web.questEditor.actions

import world.phantasmal.observable.change
import world.phantasmal.web.core.actions.Action
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.web.questEditor.models.QuestEntityModel

class TranslateEntityAction(
    private val setSelectedEntity: (QuestEntityModel<*, *>) -> Unit,
    private val setEntitySection: (Int) -> Unit,
    private val entity: QuestEntityModel<*, *>,
    private val newSection: Int?,
    private val oldSection: Int?,
    private val newPosition: Vector3,
    private val oldPosition: Vector3,
) : Action {
    override val description: String = "Move ${entity.type.simpleName}"

    override fun execute() {
        change {
            setSelectedEntity(entity)

            newSection?.let(setEntitySection)

            entity.setPosition(newPosition)
        }
    }

    override fun undo() {
        change {
            setSelectedEntity(entity)

            oldSection?.let(setEntitySection)

            entity.setPosition(oldPosition)
        }
    }
}
