package world.phantasmal.observable.value

import world.phantasmal.observable.ObservableAndEmit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * In these tests the direct dependency of the [FlatMappedVal] changes.
 */
class FlatMappedValDependentValEmitsTests : RegularValTests() {
    /**
     * This is a regression test, it's important that this exact sequence of statements stays the
     * same.
     */
    @Test
    fun emits_a_change_when_its_direct_val_dependency_changes() = test {
        val v = SimpleVal(SimpleVal(7))
        val fv = FlatMappedVal(listOf(v)) { v.value }
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

    override fun create(): ObservableAndEmit<*, FlatMappedVal<*>> {
        val v = SimpleVal(SimpleVal(5))
        val value = FlatMappedVal(listOf(v)) { v.value }
        return ObservableAndEmit(value) { v.value = SimpleVal(v.value.value + 5) }
    }

    override fun <T> createWithValue(value: T): FlatMappedVal<T> {
        val v = SimpleVal(SimpleVal(value))
        return FlatMappedVal(listOf(v)) { v.value }
    }
}
