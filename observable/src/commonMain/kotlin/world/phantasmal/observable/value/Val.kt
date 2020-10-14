package world.phantasmal.observable.value

import world.phantasmal.core.disposable.Scope
import world.phantasmal.observable.Observable
import kotlin.reflect.KProperty

/**
 * An observable with the notion of a current [value].
 */
interface Val<out T> : Observable<T> {
    val value: T

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    /**
     * @param callNow Call [observer] immediately with the current [mutableVal].
     */
    fun observe(scope: Scope, callNow: Boolean = false, observer: ValObserver<T>)

    fun <R> transform(transform: (T) -> R): Val<R> =
        DependentVal(listOf(this)) { transform(value) }

    fun <T2, R> transform(v2: Val<T2>, transform: (T, T2) -> R): Val<R> =
        DependentVal(listOf(this, v2)) { transform(value, v2.value) }

    fun <R> flatTransform(transform: (T) -> Val<R>): Val<R> =
        TODO()
}
