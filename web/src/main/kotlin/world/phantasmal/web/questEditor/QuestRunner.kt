package world.phantasmal.web.questEditor

import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.falseCell

/**
 * Orchestrates everything related to emulating a quest run. Drives a VirtualMachine and
 * delegates to Debugger.
 */
class QuestRunner {
    val running: Cell<Boolean> = falseCell()

    fun stop() {
        // TODO
    }
}
