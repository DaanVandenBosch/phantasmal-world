package world.phantasmal.observable.value

class DelegatingVal<T>(
    private val getter: () -> T,
    private val setter: (T) -> Unit,
) : AbstractVal<T>(), MutableVal<T> {
    override var value: T
        get() = getter()
        set(value) {
            val oldValue = getter()

            if (value != oldValue) {
                setter(value)
                emit(oldValue)
            }
        }
}
