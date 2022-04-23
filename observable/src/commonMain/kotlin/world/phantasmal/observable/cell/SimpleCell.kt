package world.phantasmal.observable.cell

import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.ChangeManager

class SimpleCell<T>(value: T) : AbstractCell<T>(), MutableCell<T> {
    override var value: T = value
        set(value) {
            if (value != field) {
                emitMightChange()

                field = value

                ChangeManager.changed(this)
            }
        }

    override fun emitDependencyChanged() {
        emitDependencyChangedEvent(ChangeEvent(value))
    }
}
