package world.phantasmal.observable.value

class SimpleVal<T>(value: T) : AbstractVal<T>(), MutableVal<T> {
    override var value: T = value
        set(value) {
            if (value != field) {
                val oldValue = field
                field = value
                emit(oldValue)
            }
        }
}
