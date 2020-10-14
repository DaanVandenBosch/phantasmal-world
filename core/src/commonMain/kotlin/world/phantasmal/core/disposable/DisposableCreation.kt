package world.phantasmal.core.disposable

fun Scope.disposable(dispose: () -> Unit): Disposable = SimpleDisposable(this, dispose)
