package world.phantasmal.observable.value

import kotlin.reflect.KProperty

interface MutableVal<T> : Val<T> {
    override var value: T

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}
