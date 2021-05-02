package world.phantasmal.core.disposable

/**
 * A global count is kept of all undisposed instances of this class.
 * This count can be used to find memory leaks.
 */
abstract class TrackedDisposable : Disposable {
    var disposed = false
        private set

    init {
        @Suppress("LeakingThis")
        track(this)
    }

    override fun dispose() {
        if (!disposed) {
            disposed = true
            untrack(this)
        }
    }

    companion object {
        private const val DISPOSABLE_PRINT_COUNT = 10

        var disposables: MutableSet<Disposable> = mutableSetOf()
        var trackPrecise = false
        var disposableCount: Int = 0
            private set

        inline fun checkNoLeaks(trackPrecise: Boolean = false, block: () -> Unit) {
            val initialCount = disposableCount
            val initialTrackPrecise = this.trackPrecise
            val initialDisposables = disposables

            this.trackPrecise = trackPrecise
            disposables = mutableSetOf()

            try {
                block()
                checkLeakCountZero(disposableCount - initialCount)
            } finally {
                this.trackPrecise = initialTrackPrecise
                disposables = initialDisposables
            }
        }

        fun track(disposable: Disposable) {
            disposableCount++

            if (trackPrecise) {
                disposables.add(disposable)
            }
        }

        fun untrack(disposable: Disposable) {
            disposableCount--

            if (trackPrecise) {
                disposables.remove(disposable)
            }
        }

        fun checkLeakCountZero(leakCount: Int) {
            check(leakCount == 0) {
                buildString {
                    append("$leakCount TrackedDisposables were leaked")

                    if (trackPrecise) {
                        append(": ")
                        disposables.take(DISPOSABLE_PRINT_COUNT).joinTo(this) {
                            it::class.simpleName ?: "Anonymous"
                        }

                        if (disposables.size > DISPOSABLE_PRINT_COUNT) {
                            append(",..")
                        }
                    }

                    append(".")
                }
            }
        }
    }
}
