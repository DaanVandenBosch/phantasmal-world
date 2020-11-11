package world.phantasmal.web.questEditor

import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal

/**
 * Orchestrates everything related to emulating a quest run. Drives a [VirtualMachine] and
 * delegates to [Debugger].
 */
class QuestRunner {
    val running: Val<Boolean> = falseVal()
}
