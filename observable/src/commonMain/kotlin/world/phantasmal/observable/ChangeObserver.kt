package world.phantasmal.observable

typealias ChangeObserver<T> = (ChangeEvent<T>) -> Unit

open class ChangeEvent<out T>(
    /**
     * The observable's new value.
     */
    val value: T,
) {
    operator fun component1() = value
}
