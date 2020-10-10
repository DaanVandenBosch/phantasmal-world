package world.phantasmal.observable.value

import world.phantasmal.observable.test.TestSuite
import kotlin.test.*

class ValCreationTests : TestSuite() {
    @Test
    fun test_value() {
        assertEquals(7, value(7).value)
    }

    @Test
    fun test_trueVal() {
        assertTrue(trueVal().value)
    }

    @Test
    fun test_falseVal() {
        assertFalse(falseVal().value)
    }

    @Test
    fun test_nullVal() {
        assertNull(nullVal().value)
    }

    @Test
    fun test_mutableVal_with_initial_value() {
        val v = mutableVal(17)

        assertEquals(17, v.value)

        v.value = 201

        assertEquals(201, v.value)
    }

    @Test
    fun test_mutableVal_with_getter_and_setter() {
        var x = 17
        val v = mutableVal({ x }, { x = it })

        assertEquals(17, v.value)

        v.value = 201

        assertEquals(201, v.value)
    }
}
