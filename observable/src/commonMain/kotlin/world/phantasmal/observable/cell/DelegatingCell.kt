package world.phantasmal.observable.cell

import world.phantasmal.observable.ChangeEvent

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

                emitChanged(ChangeEvent(value))
            }
        }
}
