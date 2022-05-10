package world.phantasmal.core.disposable

/**
 * Subclasses of this class are automatically tracked. Subclasses are required to call
 * super.[dispose].
 */
abstract class TrackedDisposable : Disposable {
    var disposed = false
        private set

    init {
        // Suppress this warning, because track simply adds this disposable to a set at this point.
        @Suppress("LeakingThis")
        DisposableTracking.track(this)
    }

    override fun dispose() {
        require(!disposed) {
            "${this::class.simpleName ?: "(Anonymous class)"} already disposed."
        }

        disposed = true
        DisposableTracking.disposed(this)
    }
}
