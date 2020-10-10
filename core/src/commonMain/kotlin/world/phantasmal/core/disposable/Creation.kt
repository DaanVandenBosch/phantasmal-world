package world.phantasmal.core.disposable

fun disposable(dispose: () -> Unit): Disposable = SimpleDisposable(dispose)
