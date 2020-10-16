package world.phantasmal.observable.test

import world.phantasmal.core.disposable.DisposableScope
import world.phantasmal.core.disposable.Scope
import kotlin.coroutines.EmptyCoroutineContext

fun withScope(block: (Scope) -> Unit) {
    val scope = DisposableScope(EmptyCoroutineContext)

    try {
        block(scope)
    } finally {
        scope.dispose()
    }
}
