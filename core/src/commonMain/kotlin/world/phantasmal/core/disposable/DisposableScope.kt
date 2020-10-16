package world.phantasmal.core.disposable

import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

class DisposableScope(override val coroutineContext: CoroutineContext) : Scope, Disposable {
    private val disposables = mutableListOf<Disposable>()
    private var disposed = false

    /**
     * The amount of held disposables.
     */
    val size: Int get() = disposables.size

    override fun scope(): Scope = DisposableScope(coroutineContext + SupervisorJob()).also(::add)

    override fun add(disposable: Disposable) {
        require(!disposed) { "Scope already disposed." }

        disposables.add(disposable)
    }

    /**
     * Add 0 or more disposables.
     */
    fun addAll(disposables: Iterable<Disposable>) {
        require(!disposed) { "Scope already disposed." }

        this.disposables.addAll(disposables)
    }

    /**
     * Add 0 or more disposables.
     */
    fun addAll(vararg disposables: Disposable) {
        require(!disposed) { "Scope already disposed." }

        this.disposables.addAll(disposables)
    }

    fun isEmpty(): Boolean = disposables.isEmpty()

    /**
     * Removes and disposes the given [disposable].
     */
    fun remove(disposable: Disposable) {
        disposables.remove(disposable)
        disposable.dispose()
    }

    /**
     * Removes and disposes [amount] disposables at the given [index].
     */
    fun removeAt(index: Int, amount: Int = 1) {
        repeat(amount) {
            disposables.removeAt(index).dispose()
        }
    }

    /**
     * Disposes all held disposables.
     */
    fun disposeAll() {
        disposables.forEach { it.dispose() }
        disposables.clear()
    }

    override fun dispose() {
        if (!disposed) {
            disposeAll()

            if (coroutineContext[Job] != null) {
                cancel()
            }

            disposed = true
        }
    }
}
