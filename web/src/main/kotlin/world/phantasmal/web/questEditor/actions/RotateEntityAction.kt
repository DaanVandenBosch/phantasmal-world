package world.phantasmal.web.questEditor.actions

import world.phantasmal.observable.change
import world.phantasmal.web.core.actions.Action
import world.phantasmal.web.externals.three.Euler
import world.phantasmal.web.questEditor.models.QuestEntityModel

class RotateEntityAction(
    private val setSelectedEntity: (QuestEntityModel<*, *>) -> Unit,
    private val entity: QuestEntityModel<*, *>,
    private val newRotation: Euler,
    private val oldRotation: Euler,
    private val world: Boolean,
) : Action {
    override val description: String = "Rotate ${entity.type.simpleName}"

    override fun execute() {
        change {
            setSelectedEntity(entity)

            if (world) {
                entity.setWorldRotation(newRotation)
            } else {
                entity.setRotation(newRotation)
            }
        }
    }

    override fun undo() {
        change {
            setSelectedEntity(entity)

            if (world) {
                entity.setWorldRotation(oldRotation)
            } else {
                entity.setRotation(oldRotation)
            }
        }
    }
}
