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
