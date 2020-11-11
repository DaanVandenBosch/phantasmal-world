package world.phantasmal.observable.value

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.observable.Observable
import world.phantasmal.observable.Observer
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
    fun observe(callNow: Boolean = false, observer: Observer<T>): Disposable

    /**
     * Map a transformation function over this val.
     *
     * @param transform called whenever this val changes
     */
    fun <R> map(transform: (T) -> R): Val<R> =
        MappedVal(listOf(this)) { transform(value) }

    /**
     * Map a transformation function over this val and another val.
     *
     * @param transform called whenever this val or [v2] changes
     */
    fun <T2, R> map(v2: Val<T2>, transform: (T, T2) -> R): Val<R> =
        MappedVal(listOf(this, v2)) { transform(value, v2.value) }

    /**
     * Map a transformation function over this val and two other vals.
     *
     * @param transform called whenever this val, [v2] or [v3] changes
     */
    fun <T2, T3, R> map(v2: Val<T2>, v3: Val<T3>, transform: (T, T2, T3) -> R): Val<R> =
        MappedVal(listOf(this, v2, v3)) { transform(value, v2.value, v3.value) }

    /**
     * Map a transformation function that returns a val over this val. The resulting val will change
     * when this val changes and when the val returned by [transform] changes.
     *
     * @param transform called whenever this val changes
     */
    fun <R> flatMap(transform: (T) -> Val<R>): Val<R> =
        FlatMappedVal(listOf(this)) { transform(value) }
}
