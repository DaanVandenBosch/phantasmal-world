package world.phantasmal.observable.cell

class DelegatingCell<T>(
    private val getter: () -> T,
    private val setter: (T) -> Unit,
) : AbstractCell<T>(), MutableCell<T> {
    override var value: T
        get() = getter()
        set(value) {
            val oldValue = getter()

            if (value != oldValue) {
                setter(value)
                emit()
            }
        }
}
