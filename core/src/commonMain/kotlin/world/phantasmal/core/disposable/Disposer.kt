package world.phantasmal.core.disposable

/**
 * Container for disposables. Takes ownership of all held disposables and automatically disposes
 * them when the Disposer is disposed.
 */
class Disposer : TrackedDisposable() {
    private val disposables = mutableListOf<Disposable>()

    /**
     * The amount of held disposables.
     */
    val size: Int get() = disposables.size

    /**
     * Add a single disposable and return the given disposable.
     */
    fun <T : Disposable> add(disposable: T): T {
        if (disposed) {
            disposable.dispose()
        } else {
            disposables.add(disposable)
        }

        return disposable
    }

    /**
     * Add 0 or more disposables.
     */
    fun addAll(disposables: Iterable<Disposable>) {
        if (disposed) {
            disposables.forEach { it.dispose() }
        } else {
            this.disposables.addAll(disposables)
        }
    }

    /**
     * Add 0 or more disposables.
     */
    fun addAll(vararg disposables: Disposable) {
        if (disposed) {
            disposables.forEach { it.dispose() }
        } else {
            this.disposables.addAll(disposables)
        }
    }

    fun isEmpty(): Boolean = disposables.isEmpty()

    /**
     * Removes and disposes the given [disposable].
     */
    fun remove(disposable: Disposable) {
        disposables.remove(disposable)
        disposable.dispose()
    }

    /**
     * Removes and disposes [amount] disposables at the given [index].
     */
    fun removeAt(index: Int, amount: Int = 1) {
        repeat(amount) {
            disposables.removeAt(index).dispose()
        }
    }

    /**
     * Disposes all held disposables.
     */
    fun disposeAll() {
        disposables.forEach { it.dispose() }
        disposables.clear()
    }

    override fun internalDispose() {
        disposeAll()
    }
}
