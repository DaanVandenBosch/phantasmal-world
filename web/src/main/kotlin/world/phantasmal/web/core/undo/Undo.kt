package world.phantasmal.web.core.undo

import world.phantasmal.observable.cell.Cell
import world.phantasmal.web.core.actions.Action

interface Undo {
    val canUndo: Cell<Boolean>
    val canRedo: Cell<Boolean>

    /**
     * The first action that will be undone when calling undo().
     */
    val firstUndo: Cell<Action?>

    /**
     * The first action that will be redone when calling redo().
     */
    val firstRedo: Cell<Action?>

    /**
     * True if this undo is at the point in time where the last save happened. See [savePoint].
     * If false, it should be safe to leave the application because no changes have happened since
     * the last save point (either because there were no changes or all changes have been undone).
     */
    val atSavePoint: Cell<Boolean>

    fun undo(): Boolean
    fun redo(): Boolean

    /**
     * Called when a save happens, the undo should remember this point in time and reflect whether
     * it's currently at this point in [atSavePoint].
     */
    fun savePoint()

    fun reset()
}
