package world.phantasmal.web.core.undo

import world.phantasmal.observable.value.Val
import world.phantasmal.web.core.actions.Action

interface Undo {
    val canUndo: Val<Boolean>
    val canRedo: Val<Boolean>

    /**
     * The first action that will be undone when calling undo().
     */
    val firstUndo: Val<Action?>

    /**
     * The first action that will be redone when calling redo().
     */
    val firstRedo: Val<Action?>

    /**
     * Ensures this undo is the current undo in its [UndoManager].
     */
    fun makeCurrent()
    fun undo(): Boolean
    fun redo(): Boolean
    fun reset()
}
