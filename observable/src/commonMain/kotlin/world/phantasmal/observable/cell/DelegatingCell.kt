package world.phantasmal.observable.cell

import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.ChangeManager

class DelegatingCell<T>(
    private val getter: () -> T,
    private val setter: (T) -> Unit,
) : AbstractCell<T>(), MutableCell<T> {
    override var value: T
        get() = getter()
        set(value) {
            val oldValue = getter()

            if (value != oldValue) {
                emitMightChange()

                setter(value)

                ChangeManager.changed(this)
            }
        }

    override fun emitDependencyChanged() {
        emitDependencyChangedEvent(ChangeEvent(value))
    }
}
