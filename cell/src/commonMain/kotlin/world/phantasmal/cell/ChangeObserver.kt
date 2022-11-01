package world.phantasmal.cell

typealias ChangeObserver<T> = (ChangeEvent<T>) -> Unit

/**
 * Don't keep long-lived references to change events, they may change internally after change
 * observers have been called.
 */
open class ChangeEvent<out T>(
    /**
     * The cell's new value. Don't keep long-lived references to this object, it may change after
     * change observers have been called.
     */
    val value: T,
) {
    operator fun component1() = value

    override fun toString(): String =
        "ChangeEvent($value)"
}
