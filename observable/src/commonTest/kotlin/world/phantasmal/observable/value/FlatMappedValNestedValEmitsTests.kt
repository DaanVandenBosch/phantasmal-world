package world.phantasmal.observable.value

import world.phantasmal.observable.ObservableAndEmit

/**
 * In these tests the dependency of the [FlatMappedVal]'s direct dependency changes.
 */
class FlatMappedValNestedValEmitsTests : RegularValTests() {
    override fun create(): ObservableAndEmit<*, FlatMappedVal<*>> {
        val v = SimpleVal(SimpleVal(5))
        val value = FlatMappedVal(listOf(v)) { v.value }
        return ObservableAndEmit(value) { v.value.value += 5 }
    }

    override fun <T> createWithValue(value: T): FlatMappedVal<T> {
        val v = SimpleVal(SimpleVal(value))
        return FlatMappedVal(listOf(v)) { v.value }
    }
}
