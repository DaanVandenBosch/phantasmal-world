package world.phantasmal.observable.value

import world.phantasmal.observable.ChangeEvent

class ValChangeEvent<out T>(value: T, val oldValue: T) : ChangeEvent<T>(value) {
    operator fun component2() = oldValue
}

typealias ValObserver<T> = (event: ValChangeEvent<T>) -> Unit
