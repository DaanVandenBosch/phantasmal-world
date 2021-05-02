package world.phantasmal.core.disposable

private object NopDisposable : Disposable {
    override fun dispose() {
        // Do nothing.
    }
}

fun disposable(dispose: () -> Unit): Disposable = SimpleDisposable(dispose)

/**
 * Returns a disposable that does nothing when disposed.
 */
fun nopDisposable(): Disposable = NopDisposable
