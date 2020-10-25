package world.phantasmal.core.disposable

/**
 * Objects implementing this interface should be disposed when they're not used anymore. This is to
 * avoid resource leaks.
 */
interface Disposable {
    /**
     * Releases any held resources.
     */
    fun dispose()
}

/**
 * Executes the given function on this disposable and then disposes it whether an exception is
 * thrown or not.
 *
 * @param block a function to process this [Disposable] resource.
 * @return the result of [block] invoked on this resource.
 */
inline fun <D : Disposable, R> D.use(block: (D) -> R): R {
    try {
        return block(this)
    } finally {
        dispose()
    }
}
