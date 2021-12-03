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
     * @param callNow Call [observer] immediately with the current [value].
     */
    fun observe(callNow: Boolean = false, observer: Observer<T>): Disposable
}
