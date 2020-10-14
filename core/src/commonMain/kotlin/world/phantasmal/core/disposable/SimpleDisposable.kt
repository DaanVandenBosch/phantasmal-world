package world.phantasmal.core.disposable

class SimpleDisposable(
    scope: Scope,
    private val dispose: () -> Unit,
) : TrackedDisposable(scope) {
    override fun internalDispose() {
        // Use invoke to avoid calling the dispose method instead of the dispose property.
        dispose.invoke()
    }
}
