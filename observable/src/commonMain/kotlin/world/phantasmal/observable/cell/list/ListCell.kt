package world.phantasmal.observable.cell.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.observable.cell.Cell

interface ListCell<out E> : Cell<List<E>> {
    override val value: List<E>

    val size: Cell<Int>

    val empty: Cell<Boolean>

    val notEmpty: Cell<Boolean>

    operator fun get(index: Int): E = value[index]

    fun observeList(callNow: Boolean = false, observer: ListObserver<E>): Disposable

    operator fun contains(element: @UnsafeVariance E): Boolean = element in value
}
