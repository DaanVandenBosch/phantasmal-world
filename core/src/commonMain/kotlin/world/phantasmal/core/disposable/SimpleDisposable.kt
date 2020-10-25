package world.phantasmal.core.disposable

class SimpleDisposable(
    private val dispose: () -> Unit,
) : TrackedDisposable() {
    override fun internalDispose() {
        // Use invoke to avoid calling the dispose method instead of the dispose property.
        dispose.invoke()
    }
}
