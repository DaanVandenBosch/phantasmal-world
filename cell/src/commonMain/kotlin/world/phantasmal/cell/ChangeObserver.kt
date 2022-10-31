package world.phantasmal.cell

typealias ChangeObserver<T> = (ChangeEvent<T>) -> Unit

open class ChangeEvent<out T>(
    /**
     * The cell's new value. Don't keep long-lived references to this object, it may change after
     * change observers have been called.
     */
    val value: T,
) {
    operator fun component1() = value
}
