package world.phantasmal.cell

typealias ChangeObserver<T> = (ChangeEvent<T>) -> Unit

open class ChangeEvent<out T>(
    /** The cell's new value. */
    val value: T,
) {
    operator fun component1() = value
}
