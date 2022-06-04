package world.phantasmal.cell

import kotlin.reflect.KProperty

interface MutableCell<T> : Cell<T> {
    override var value: T

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}
