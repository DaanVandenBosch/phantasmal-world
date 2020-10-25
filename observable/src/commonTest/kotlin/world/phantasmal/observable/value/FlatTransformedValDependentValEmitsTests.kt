package world.phantasmal.observable.value

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * In these tests the direct dependency of the [FlatTransformedVal] changes.
 */
class FlatTransformedValDependentValEmitsTests : RegularValTests() {
    /**
     * This is a regression test, it's important that this exact sequence of statements stays the
     * same.
     */
    @Test
    fun emits_a_change_when_its_direct_val_dependency_changes() {
        val v = SimpleVal(SimpleVal(7))
        val fv = FlatTransformedVal(listOf(v)) { v.value }
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

    override fun create(): ValAndEmit<*> {
        val v = SimpleVal(SimpleVal(5))
        val value = FlatTransformedVal(listOf(v)) { v.value }
        return ValAndEmit(value) { v.value = SimpleVal(v.value.value + 5) }
    }

    override fun createBoolean(bool: Boolean): ValAndEmit<Boolean> {
        val v = SimpleVal(SimpleVal(bool))
        val value = FlatTransformedVal(listOf(v)) { v.value }
        return ValAndEmit(value) { v.value = SimpleVal(!v.value.value) }
    }
}
