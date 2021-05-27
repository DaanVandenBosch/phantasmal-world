package world.phantasmal.observable.cell

import world.phantasmal.observable.ChangeEvent

class SimpleCell<T>(value: T) : AbstractCell<T>(), MutableCell<T> {
    override var value: T = value
        set(value) {
            if (value != field) {
                emitMightChange()

                field = value

                emitChanged(ChangeEvent(value))
            }
        }
}
