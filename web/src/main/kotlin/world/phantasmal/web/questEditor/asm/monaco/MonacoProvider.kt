package world.phantasmal.web.questEditor.asm.monaco

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

abstract class MonacoProvider {
    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
