package world.phantasmal.core.disposable

/**
 * A global count is kept of all undisposed instances of this class.
 * This count can be used to find memory leaks.
 */
abstract class TrackedDisposable(scope: Scope) : Disposable {
    var disposed = false
        private set

    init {
        @Suppress("LeakingThis")
        scope.add(this)

        disposableCount++
    }

    final override fun dispose() {
        if (!disposed) {
            disposed = true
            disposableCount--
            internalDispose()
        }
    }

    protected open fun internalDispose() {
        // Do nothing.
    }

    companion object {
        var disposableCount: Int = 0
            private set

        fun checkNoLeaks(block: () -> Unit) {
            val count = disposableCount

            try {
                block()
            } finally {
                check(count == disposableCount) { "TrackedDisposables were leaked." }
            }
        }
    }
}
