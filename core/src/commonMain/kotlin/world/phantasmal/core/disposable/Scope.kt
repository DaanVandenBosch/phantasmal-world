package world.phantasmal.core.disposable

import kotlinx.coroutines.CoroutineScope

/**
 * Container for disposables. Takes ownership of all held disposables and automatically disposes
 * them when the Scope is disposed.
 */
interface Scope: CoroutineScope {
    fun add(disposable: Disposable)

    /**
     * Creates a sub-scope of this scope.
     */
    fun scope(): Scope
}
