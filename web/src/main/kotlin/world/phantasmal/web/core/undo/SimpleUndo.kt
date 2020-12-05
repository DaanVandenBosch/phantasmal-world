package world.phantasmal.web.core.undo

import world.phantasmal.observable.value.*
import world.phantasmal.web.core.actions.Action

/**
 * Simply contains a single action. [canUndo] and [canRedo] must be managed manually.
 */
class SimpleUndo(
    undoManager: UndoManager,
    private val description: String,
    undo: () -> Unit,
    redo: () -> Unit,
) : Undo {
    private val action = object : Action {
        override val description: String = this@SimpleUndo.description

        override fun execute() {
            redo()
        }

        override fun undo() {
            undo()
        }
    }

    override val canUndo: MutableVal<Boolean> = mutableVal(false)
    override val canRedo: MutableVal<Boolean> = mutableVal(false)

    override val firstUndo: Val<Action?> = canUndo.map { if (it) action else null }
    override val firstRedo: Val<Action?> = canRedo.map { if (it) action else null }

    init {
        undoManager.addUndo(this)
    }

    override fun undo(): Boolean =
        if (canUndo.value) {
            action.undo()
            true
        } else {
            false
        }

    override fun redo(): Boolean =
        if (canRedo.value) {
            action.execute()
            true
        } else {
            false
        }

    override fun reset() {
        canUndo.value = false
        canRedo.value = false
    }
}
