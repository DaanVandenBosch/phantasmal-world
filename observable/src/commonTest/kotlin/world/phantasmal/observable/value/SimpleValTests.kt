package world.phantasmal.observable.value

import world.phantasmal.observable.ObservableAndEmit

class SimpleValTests : RegularValTests() {
    override fun create(): ObservableAndEmit<*, SimpleVal<*>> {
        val value = SimpleVal(1)
        return ObservableAndEmit(value) { value.value += 2 }
    }

    override fun <T> createWithValue(value: T): SimpleVal<T> =
        SimpleVal(value)
}
