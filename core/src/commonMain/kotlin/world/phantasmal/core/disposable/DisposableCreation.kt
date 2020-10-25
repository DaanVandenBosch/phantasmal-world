package world.phantasmal.core.disposable

private object StubDisposable : Disposable {
    override fun dispose() {
        // Do nothing.
    }
}

fun disposable(dispose: () -> Unit): Disposable = SimpleDisposable(dispose)

fun stubDisposable(): Disposable = StubDisposable
