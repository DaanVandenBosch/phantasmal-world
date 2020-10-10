package world.phantasmal.observable.value.list

import world.phantasmal.observable.value.MutableVal
import kotlin.reflect.KProperty

interface MutableListVal<E> : ListVal<E>, MutableVal<List<E>>, MutableList<E> {
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): MutableList<E> = this
}
