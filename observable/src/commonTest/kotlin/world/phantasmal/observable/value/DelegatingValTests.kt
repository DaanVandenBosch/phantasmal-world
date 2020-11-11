package world.phantasmal.observable.value

import world.phantasmal.observable.ObservableAndEmit

class DelegatingValTests : RegularValTests() {
    override fun create(): ObservableAndEmit<*, DelegatingVal<*>> {
        var v = 0
        val value = DelegatingVal({ v }, { v = it })
        return ObservableAndEmit(value) { value.value += 2 }
    }

    override fun <T> createWithValue(value: T): DelegatingVal<T> {
        var v = value
        return DelegatingVal({ v }, { v = it })
    }
}
