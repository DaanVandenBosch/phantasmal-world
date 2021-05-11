package world.phantasmal.observable.cell

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.observable.Observable
import world.phantasmal.observable.Observer
import world.phantasmal.observable.cell.list.DependentListCell
import world.phantasmal.observable.cell.list.ListCell
import kotlin.reflect.KProperty

/**
 * An observable with the notion of a current [value].
 */
interface Cell<out T> : Observable<T> {
    val value: T

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    /**
     * @param callNow Call [observer] immediately with the current [mutableCell].
     */
    fun observe(callNow: Boolean = false, observer: Observer<T>): Disposable

    /**
     * Map a transformation function over this cell.
     *
     * @param transform called whenever this cell changes
     */
    fun <R> map(transform: (T) -> R): Cell<R> =
        DependentCell(this) { transform(value) }

    fun <R> mapToList(transform: (T) -> List<R>): ListCell<R> =
        DependentListCell(this) { transform(value) }

    /**
     * Map a transformation function that returns a cell over this cell. The resulting cell will
     * change when this cell changes and when the cell returned by [transform] changes.
     *
     * @param transform called whenever this cell changes
     */
    fun <R> flatMap(transform: (T) -> Cell<R>): Cell<R> =
        FlatteningDependentCell(this) { transform(value) }

    fun <R> flatMapNull(transform: (T) -> Cell<R>?): Cell<R?> =
        FlatteningDependentCell(this) { transform(value) ?: nullCell() }

    fun isNull(): Cell<Boolean> =
        map { it == null }

    fun isNotNull(): Cell<Boolean> =
        map { it != null }
}
