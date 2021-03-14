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
        DependentVal(listOf(this)) { transform(value) }

    /**
     * Map a transformation function that returns a val over this val. The resulting val will change
     * when this val changes and when the val returned by [transform] changes.
     *
     * @param transform called whenever this val changes
     */
    fun <R> flatMap(transform: (T) -> Val<R>): Val<R> =
        FlatMappedVal(listOf(this)) { transform(value) }

    fun <R> flatMapNull(transform: (T) -> Val<R>?): Val<R?> =
        FlatMappedVal(listOf(this)) { transform(value) ?: nullVal() }

    fun isNull(): Val<Boolean> =
        map { it == null }

    fun isNotNull(): Val<Boolean> =
        map { it != null }
}
