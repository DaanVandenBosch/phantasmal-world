package world.phantasmal.observable.value

import kotlin.test.*

/**
 * Test suite for all [Val] implementations that aren't ListVals. There is a subclass of this suite
 * for every non-ListVal [Val] implementation.
 */
interface RegularValTests : ValTests {
    fun <T> createWithValue(value: T): Val<T>

    /**
     * [Val.value] should correctly reflect changes even when the [Val] has no observers. Typically
     * this means that the val's value is not updated in real time, only when it is queried.
     */
    @Test
    fun reflects_changes_without_observers() = test {
        val p = createProvider()

        var old: Any?

        repeat(5) {
            // Value should change after emit.
            old = p.observable.value

            p.emit()

            val new = p.observable.value

            assertNotEquals(old, new)

            // Value should not change when emit hasn't been called since the last access.
            assertEquals(new, p.observable.value)

            old = new
        }
    }

    @Test
    fun val_convenience_methods() = test {
        listOf(Any(), null).forEach { any ->
            val anyVal = createWithValue(any)

            // Test the test setup first.
            assertEquals(any, anyVal.value)

            // Test `isNull`.
            assertEquals(any == null, anyVal.isNull().value)

            // Test `isNotNull`.
            assertEquals(any != null, anyVal.isNotNull().value)
        }
    }

    @Test
    fun val_generic_extensions() = test {
        listOf(Any(), null).forEach { any ->
            val anyVal = createWithValue(any)

            // Test the test setup first.
            assertEquals(any, anyVal.value)

            // Test `orElse`.
            assertEquals(any ?: "default", anyVal.orElse { "default" }.value)
        }

        fun <T> testEqNe(a: T, b: T) {
            val aVal = createWithValue(a)
            val bVal = createWithValue(b)

            // Test the test setup first.
            assertEquals(a, aVal.value)
            assertEquals(b, bVal.value)

            // Test `eq`.
            assertEquals(a == b, (aVal eq b).value)
            assertEquals(a == b, (aVal eq bVal).value)

            // Test `ne`.
            assertEquals(a != b, (aVal ne b).value)
            assertEquals(a != b, (aVal ne bVal).value)
        }

        testEqNe(10, 10)
        testEqNe(5, 99)
        testEqNe("a", "a")
        testEqNe("x", "y")
    }

    @Test
    fun val_comparable_extensions() = test {
        fun <T : Comparable<T>> comparable_tests(a: T, b: T) {
            val aVal = createWithValue(a)
            val bVal = createWithValue(b)

            // Test the test setup first.
            assertEquals(a, aVal.value)
            assertEquals(b, bVal.value)

            // Test `gt`.
            assertEquals(a > b, (aVal gt b).value)
            assertEquals(a > b, (aVal gt bVal).value)

            // Test `lt`.
            assertEquals(a < b, (aVal lt b).value)
            assertEquals(a < b, (aVal lt bVal).value)
        }

        comparable_tests(10, 10)
        comparable_tests(7.0, 5.0)
        comparable_tests((5000).toShort(), (7000).toShort())
    }

    @Test
    fun val_boolean_extensions() = test {
        listOf(true, false).forEach { bool ->
            val boolVal = createWithValue(bool)

            // Test the test setup first.
            assertEquals(bool, boolVal.value)

            // Test `and`.
            assertEquals(bool, (boolVal and trueVal()).value)
            assertFalse((boolVal and falseVal()).value)

            // Test `or`.
            assertTrue((boolVal or trueVal()).value)
            assertEquals(bool, (boolVal or falseVal()).value)

            // Test `xor`.
            assertEquals(!bool, (boolVal xor trueVal()).value)
            assertEquals(bool, (boolVal xor falseVal()).value)

            // Test `!` (unary not).
            assertEquals(!bool, (!boolVal).value)
        }
    }

    @Test
    fun val_string_extensions() = test {
        listOf("", "  ", "\t\t", "non-empty-non-blank").forEach { string ->
            val stringVal = createWithValue(string)

            // Test the test setup first.
            assertEquals(string, stringVal.value)

            // Test `isEmpty`.
            assertEquals(string.isEmpty(), stringVal.isEmpty().value)

            // Test `isNotEmpty`.
            assertEquals(string.isNotEmpty(), stringVal.isNotEmpty().value)

            // Test `isBlank`.
            assertEquals(string.isBlank(), stringVal.isBlank().value)

            // Test `isNotBlank`.
            assertEquals(string.isNotBlank(), stringVal.isNotBlank().value)
        }
    }
}
