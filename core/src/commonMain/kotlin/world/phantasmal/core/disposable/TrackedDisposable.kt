package world.phantasmal.core.disposable

/**
 * A global count is kept of all undisposed instances of this class.
 * This count can be used to find memory leaks.
 */
abstract class TrackedDisposable : Disposable {
    var disposed = false
        private set

    init {
        disposableCount++
    }

    final override fun dispose() {
        if (!disposed) {
            disposed = true
            disposableCount--
            internalDispose()
        }
    }

    protected abstract fun internalDispose()

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
