package world.phantasmal.web.core.undo

import world.phantasmal.observable.cell.*
import world.phantasmal.observable.cell.list.mutableListCell
import world.phantasmal.web.core.actions.Action

/**
 * Full-fledged linear undo/redo implementation.
 */
class UndoStack(manager: UndoManager) : Undo {
    private val stack = mutableListCell<Action>()

    /**
     * The index where new actions are inserted. If not equal to the [stack]'s size, points to the
     * action that will be redone when calling [redo].
     */
    private val index = mutableCell(0)
    private val savePointIndex = mutableCell(0)
    private var undoingOrRedoing = false

    override val canUndo: Cell<Boolean> = index gt 0

    override val canRedo: Cell<Boolean> = map(stack, index) { stack, index -> index < stack.size }

    override val firstUndo: Cell<Action?> = index.map { stack.value.getOrNull(it - 1) }

    override val firstRedo: Cell<Action?> = index.map { stack.value.getOrNull(it) }

    override val atSavePoint: Cell<Boolean> = index eq savePointIndex

    init {
        manager.addUndo(this)
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

    override fun savePoint() {
        savePointIndex.value = index.value
    }

    override fun reset() {
        stack.clear()
        index.value = 0
        savePointIndex.value = 0
    }
}
