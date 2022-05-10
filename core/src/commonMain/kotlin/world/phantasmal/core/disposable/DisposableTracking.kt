package world.phantasmal.core.disposable

private const val DISPOSABLE_PRINT_COUNT = 10

/**
 * A global count is kept of all undisposed, tracked disposables. This count is used to find memory
 * leaks.
 *
 * Tracking is not thread-safe.
 */
object DisposableTracking {
    var globalDisposableTracker: DisposableTracker? = null

    inline fun checkNoLeaks(block: () -> Unit) {
        // Remember the old tracker to make this function reentrant.
        val initialTracker = globalDisposableTracker

        try {
            val tracker = DisposableTracker()
            globalDisposableTracker = tracker

            block()

            check(globalDisposableTracker === tracker) {
                "Tracker was changed."
            }

            tracker.checkLeakCountZero()
        } finally {
            globalDisposableTracker = initialTracker
        }
    }

    fun track(disposable: Disposable) {
        globalDisposableTracker?.track(disposable)
    }

    fun disposed(disposable: Disposable) {
        globalDisposableTracker?.disposed(disposable)
    }
}

class DisposableTracker {
    /**
     * Mapping of tracked disposables to their liveness state. True means live, false means
     * disposed.
     */
    private var disposables: MutableMap<Disposable, Boolean> = mutableMapOf()

    /** Keep count as optimization for check in [checkLeakCountZero]. */
    private var liveCount = 0

    fun track(disposable: Disposable) {
        val live = disposables.put(disposable, true)

        if (live == true) {
            error("${getName(disposable)} was already tracked.")
        } else if (live == false) {
            disposables[disposable] = false
            error("${getName(disposable)} was already tracked and then disposed.")
        }

        liveCount++
    }

    fun disposed(disposable: Disposable) {
        val live = disposables.put(disposable, false)

        if (live == null) {
            disposables.remove(disposable)
            error("${getName(disposable)} was never tracked.")
        } else if (!live) {
            error("${getName(disposable)} was already disposed.")
        }

        liveCount--
    }

    fun checkLeakCountZero() {
        check(liveCount == 0) {
            buildString {
                append(liveCount)
                append(" Disposables were leaked: ")

                val leakedDisposables = disposables.entries
                    .asSequence()
                    .filter { (_, live) -> live }
                    .take(DISPOSABLE_PRINT_COUNT)
                    .map { (disposable, _) -> disposable }
                    .toList()

                check(liveCount >= leakedDisposables.size)

                leakedDisposables.joinTo(this, transform = ::getName)

                if (liveCount > DISPOSABLE_PRINT_COUNT) {
                    append(",...")
                } else {
                    append(".")
                }
            }
        }
    }
}

private fun getName(disposable: Disposable): String =
    disposable::class.simpleName ?: "(anonymous class)"
