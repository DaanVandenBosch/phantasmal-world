package world.phantasmal.observable

open class ChangeEvent<out T>(val value: T) {
    operator fun component1() = value
}

typealias Observer<T> = (event: ChangeEvent<T>) -> Unit
