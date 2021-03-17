package world.phantasmal.observable.value

import world.phantasmal.observable.ObservableAndEmit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * In these tests the direct dependency of the [FlatteningDependentVal] changes.
 */
class FlatteningDependentValDependentValEmitsTests : RegularValTests() {
    /**
     * This is a regression test, it's important that this exact sequence of statements stays the
     * same.
     */
    @Test
    fun emits_a_change_when_its_direct_val_dependency_changes() = test {
        val v = SimpleVal(SimpleVal(7))
        val fv = FlatteningDependentVal(listOf(v)) { v.value }
        var observedValue: Int? = null

        disposer.add(
            fv.observe { observedValue = it.value }
        )

        assertNull(observedValue)

        v.value.value = 99

        assertEquals(99, observedValue)

        v.value = SimpleVal(7)

        assertEquals(7, observedValue)
    }

    override fun create(): ObservableAndEmit<*, FlatteningDependentVal<*>> {
        val v = SimpleVal(SimpleVal(5))
        val value = FlatteningDependentVal(listOf(v)) { v.value }
        return ObservableAndEmit(value) { v.value = SimpleVal(v.value.value + 5) }
    }

    override fun <T> createWithValue(value: T): FlatteningDependentVal<T> {
        val v = SimpleVal(SimpleVal(value))
        return FlatteningDependentVal(listOf(v)) { v.value }
    }
}
