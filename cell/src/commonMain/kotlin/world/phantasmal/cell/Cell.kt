package world.phantasmal.cell

import world.phantasmal.core.disposable.Disposable
import kotlin.reflect.KProperty

/**
 * A [value] that can change over time.
 */
interface Cell<out T> : Dependency<T> {
    val value: T

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    /**
     * [observer] will be called whenever this cell changes.
     */
    fun observeChange(observer: ChangeObserver<T>): Disposable
}
