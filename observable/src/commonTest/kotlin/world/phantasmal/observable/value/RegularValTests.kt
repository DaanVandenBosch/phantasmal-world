package world.phantasmal.observable.value

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test suite for all [Val] implementations that aren't ListVals. There is a subclass of this suite
 * for every non-ListVal [Val] implementation.
 */
abstract class RegularValTests : ValTests() {
    protected abstract fun createBoolean(bool: Boolean): ValAndEmit<Boolean>

    @Test
    fun val_boolean_extensions() {
        listOf(true, false).forEach { bool ->
            val (value) = createBoolean(bool)

            // Test the test setup first.
            assertEquals(bool, value.value)

            // Test `and`.
            assertEquals(bool, (value and trueVal()).value)
            assertFalse((value and falseVal()).value)

            // Test `or`.
            assertTrue((value or trueVal()).value)
            assertEquals(bool, (value or falseVal()).value)

            // Test `xor`.
            assertEquals(!bool, (value xor trueVal()).value)
            assertEquals(bool, (value xor falseVal()).value)

            // Test `!` (unary not).
            assertEquals(!bool, (!value).value)
        }
    }
}
