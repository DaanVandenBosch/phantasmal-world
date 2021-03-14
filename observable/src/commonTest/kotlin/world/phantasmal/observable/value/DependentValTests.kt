package world.phantasmal.observable.value

import world.phantasmal.observable.ObservableAndEmit

class DependentValTests : RegularValTests() {
    override fun create(): ObservableAndEmit<*, DependentVal<*>> {
        val v = SimpleVal(0)
        val value = DependentVal(listOf(v)) { 2 * v.value }
        return ObservableAndEmit(value) { v.value += 2 }
    }

    override fun <T> createWithValue(value: T): DependentVal<T> {
        val v = SimpleVal(value)
        return DependentVal(listOf(v)) { v.value }
    }
}
