package world.phantasmal.core.disposable

/**
 * Abstract utility class for classes that need to keep track of many disposables.
 */
abstract class DisposableContainer : TrackedDisposable() {
    private val disposer = Disposer()

    override fun internalDispose() {
        disposer.dispose()
    }

    protected fun <T : Disposable> addDisposable(disposable: T): T {
        return disposer.add(disposable);
    }

    protected fun addDisposables(vararg disposables: Disposable) {
        disposer.addAll(*disposables);
    }

    protected fun removeDisposable(disposable: Disposable) =
        disposer.remove(disposable)
}
