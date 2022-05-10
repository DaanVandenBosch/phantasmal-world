package world.phantasmal.observable.cell

import world.phantasmal.observable.ChangeEvent

class DelegatingCell<T>(
    private val getter: () -> T,
    private val setter: (T) -> Unit,
) : AbstractCell<T>(), MutableCell<T> {
    override var value: T = getter()
        set(value) {
            setter(value)
            val newValue = getter()

            if (newValue != field) {
                applyChange {
                    field = newValue
                    changeEvent = ChangeEvent(newValue)
                }
            }
        }

    override var changeEvent: ChangeEvent<T>? = null
        private set
}
