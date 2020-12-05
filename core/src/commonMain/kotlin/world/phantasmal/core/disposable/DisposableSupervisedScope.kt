package world.phantasmal.core.disposable

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

class DisposableSupervisedScope(
    private val kClass: KClass<*>,
    context: CoroutineContext,
) : TrackedDisposable(), CoroutineScope by CoroutineScope(SupervisorJob() + context) {
    override fun internalDispose() {
        cancel("${kClass.simpleName} disposed.")
        super.internalDispose()
    }
}
