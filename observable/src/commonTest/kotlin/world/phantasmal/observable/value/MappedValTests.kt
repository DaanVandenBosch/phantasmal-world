package world.phantasmal.observable.value

import world.phantasmal.observable.ObservableAndEmit

class MappedValTests : RegularValTests() {
    override fun create(): ObservableAndEmit<*, MappedVal<*>> {
        val v = SimpleVal(0)
        val value = MappedVal(listOf(v)) { 2 * v.value }
        return ObservableAndEmit(value) { v.value += 2 }
    }

    override fun <T> createWithValue(value: T): MappedVal<T> {
        val v = SimpleVal(value)
        return MappedVal(listOf(v)) { v.value }
    }
}
