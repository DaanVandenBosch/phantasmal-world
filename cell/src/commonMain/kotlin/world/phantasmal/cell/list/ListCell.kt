package world.phantasmal.cell.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.cell.Cell

interface ListCell<out E> : Cell<List<E>> {
    override val value: List<E>

    override val changeEvent: ListChangeEvent<E>?

    val size: Cell<Int>

    val empty: Cell<Boolean>

    val notEmpty: Cell<Boolean>

    operator fun get(index: Int): E = value[index]

    /**
     * List variant of [Cell.observeChange].
     */
    // Exists solely because function parameters are invariant.
    fun observeListChange(observer: ListChangeObserver<E>): Disposable

    operator fun contains(element: @UnsafeVariance E): Boolean = element in value
}
