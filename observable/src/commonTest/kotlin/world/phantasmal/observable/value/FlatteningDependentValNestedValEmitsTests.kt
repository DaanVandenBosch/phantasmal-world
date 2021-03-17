package world.phantasmal.observable.value

import world.phantasmal.observable.ObservableAndEmit

/**
 * In these tests the dependency of the [FlatteningDependentVal]'s direct dependency changes.
 */
class FlatteningDependentValNestedValEmitsTests : RegularValTests() {
    override fun create(): ObservableAndEmit<*, FlatteningDependentVal<*>> {
        val v = SimpleVal(SimpleVal(5))
        val value = FlatteningDependentVal(listOf(v)) { v.value }
        return ObservableAndEmit(value) { v.value.value += 5 }
    }

    override fun <T> createWithValue(value: T): FlatteningDependentVal<T> {
        val v = SimpleVal(SimpleVal(value))
        return FlatteningDependentVal(listOf(v)) { v.value }
    }
}
