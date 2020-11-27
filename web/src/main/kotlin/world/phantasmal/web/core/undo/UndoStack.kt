package world.phantasmal.web.core.undo

import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.gt
import world.phantasmal.observable.value.list.mutableListVal
import world.phantasmal.observable.value.map
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.web.core.actions.Action

/**
 * Full-fledged linear undo/redo implementation.
 */
class UndoStack(private val manager: UndoManager) : Undo {
    private val stack = mutableListVal<Action>()

    /**
     * The index where new actions are inserted. If not equal to the [stack]'s size, points to the
     * action that will be redone when calling [redo].
     */
    private val index = mutableVal(0)
    private var undoingOrRedoing = false

    override val canUndo: Val<Boolean> = index gt 0

    override val canRedo: Val<Boolean> = map(stack, index) { stack, index -> index < stack.size }

    override val firstUndo: Val<Action?> = index.map { stack.value.getOrNull(it - 1) }

    override val firstRedo: Val<Action?> = index.map { stack.value.getOrNull(it) }

    override fun makeCurrent() {
        manager.setCurrent(this)
    }

    fun push(action: Action): Action {
        if (!undoingOrRedoing) {
            stack.splice(index.value, stack.value.size - index.value, action)
            index.value++
        }

        return action
    }

    override fun undo(): Boolean {
        if (undoingOrRedoing || !canUndo.value) return false

        try {
            undoingOrRedoing = true
            index.value -= 1
            stack[index.value].undo()
        } finally {
            undoingOrRedoing = false
            return true
        }
    }

    override fun redo(): Boolean {
        if (undoingOrRedoing || !canRedo.value) return false

        try {
            undoingOrRedoing = true
            stack[index.value].execute()
            index.value += 1
        } finally {
            undoingOrRedoing = false
            return true
        }
    }

    override fun reset() {
        stack.clear()
        index.value = 0
    }
}
