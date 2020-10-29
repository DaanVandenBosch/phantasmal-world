package world.phantasmal.observable.value

import world.phantasmal.testUtils.TestSuite
import kotlin.test.*

class ValCreationTests : TestSuite() {
    @Test
    fun test_value() = test {
        assertEquals(7, value(7).value)
    }

    @Test
    fun test_trueVal() = test {
        assertTrue(trueVal().value)
    }

    @Test
    fun test_falseVal() = test {
        assertFalse(falseVal().value)
    }

    @Test
    fun test_nullVal() = test {
        assertNull(nullVal().value)
    }

    @Test
    fun test_mutableVal_with_initial_value() = test {
        val v = mutableVal(17)

        assertEquals(17, v.value)

        v.value = 201

        assertEquals(201, v.value)
    }

    @Test
    fun test_mutableVal_with_getter_and_setter() = test {
        var x = 17
        val v = mutableVal({ x }, { x = it })

        assertEquals(17, v.value)

        v.value = 201

        assertEquals(201, v.value)
    }
}
