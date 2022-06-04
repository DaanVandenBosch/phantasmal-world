package world.phantasmal.cell

class SimpleCell<T>(value: T) : AbstractCell<T>(), MutableCell<T> {
    override var value: T = value
        set(value) {
            if (value != field) {
                applyChange {
                    field = value
                    changeEvent = ChangeEvent(value)
                }
            }
        }

    override var changeEvent: ChangeEvent<T>? = null
        private set
}
