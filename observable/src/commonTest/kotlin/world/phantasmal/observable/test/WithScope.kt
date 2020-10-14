package world.phantasmal.observable.test

import world.phantasmal.core.disposable.DisposableScope
import world.phantasmal.core.disposable.Scope

fun withScope(block: (Scope) -> Unit) {
    val scope = DisposableScope()

    try {
        block(scope)
    } finally {
        scope.dispose()
    }
}
