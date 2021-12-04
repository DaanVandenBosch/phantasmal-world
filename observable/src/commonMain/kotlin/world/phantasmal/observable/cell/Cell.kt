package world.phantasmal.observable.cell

import world.phantasmal.observable.Observable
import kotlin.reflect.KProperty

/**
 * An observable with the notion of a current [value].
 */
interface Cell<out T> : Observable<T> {
    val value: T

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value
}
