package world.phantasmal.observable.cell

class SimpleCell<T>(value: T) : AbstractCell<T>(), MutableCell<T> {
    override var value: T = value
        set(value) {
            if (value != field) {
                field = value
                emit()
            }
        }
}
