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
    protected abstract fun <T> createWithValue(value: T): Val<T>

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
        listOf(10 to 10, 5 to 99, "a" to "a", "x" to "y").forEach { (a, b) ->
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
    }

    @Test
    fun val_comparable_extensions() = test {
        listOf(
            10 to 10,
            7.0 to 5.0,
            (5000).toShort() to (7000).toShort()
        ).forEach { (a, b) ->
            @Suppress("UNCHECKED_CAST")
            a as Comparable<Any>
            @Suppress("UNCHECKED_CAST")
            b as Comparable<Any>

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
