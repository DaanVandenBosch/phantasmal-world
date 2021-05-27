package world.phantasmal.observable.cell

import kotlin.test.*

/**
 * Test suite for all [Cell] implementations that aren't ListCells. There is a subclass of this
 * suite for every non-ListCell [Cell] implementation.
 */
interface RegularCellTests : CellTests {
    fun <T> createWithValue(value: T): Cell<T>

    /**
     * [Cell.value] should correctly reflect changes even when the [Cell] has no observers.
     * Typically this means that the cell's value is not updated in real time, only when it is
     * queried.
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
        }
    }

    @Test
    fun convenience_methods() = test {
        listOf(Any(), null).forEach { any ->
            val anyCell = createWithValue(any)

            // Test the test setup first.
            assertEquals(any, anyCell.value)

            // Test `isNull`.
            assertEquals(any == null, anyCell.isNull().value)

            // Test `isNotNull`.
            assertEquals(any != null, anyCell.isNotNull().value)
        }
    }

    @Test
    fun generic_extensions() = test {
        listOf(Any(), null).forEach { any ->
            val anyCell = createWithValue(any)

            // Test the test setup first.
            assertEquals(any, anyCell.value)

            // Test `orElse`.
            assertEquals(any ?: "default", anyCell.orElse { "default" }.value)
        }

        fun <T> testEqNe(a: T, b: T) {
            val aCell = createWithValue(a)
            val bCell = createWithValue(b)

            // Test the test setup first.
            assertEquals(a, aCell.value)
            assertEquals(b, bCell.value)

            // Test `eq`.
            assertEquals(a == b, (aCell eq b).value)
            assertEquals(a == b, (aCell eq bCell).value)

            // Test `ne`.
            assertEquals(a != b, (aCell ne b).value)
            assertEquals(a != b, (aCell ne bCell).value)
        }

        testEqNe(10, 10)
        testEqNe(5, 99)
        testEqNe("a", "a")
        testEqNe("x", "y")
    }

    @Test
    fun comparable_extensions() = test {
        fun <T : Comparable<T>> comparable_tests(a: T, b: T) {
            val aCell = createWithValue(a)
            val bCell = createWithValue(b)

            // Test the test setup first.
            assertEquals(a, aCell.value)
            assertEquals(b, bCell.value)

            // Test `gt`.
            assertEquals(a > b, (aCell gt b).value)
            assertEquals(a > b, (aCell gt bCell).value)

            // Test `lt`.
            assertEquals(a < b, (aCell lt b).value)
            assertEquals(a < b, (aCell lt bCell).value)
        }

        comparable_tests(10, 10)
        comparable_tests(7.0, 5.0)
        comparable_tests((5000).toShort(), (7000).toShort())
    }

    @Test
    fun boolean_extensions() = test {
        listOf(true, false).forEach { bool ->
            val boolCell = createWithValue(bool)

            // Test the test setup first.
            assertEquals(bool, boolCell.value)

            // Test `and`.
            assertEquals(bool, (boolCell and trueCell()).value)
            assertFalse((boolCell and falseCell()).value)

            // Test `or`.
            assertTrue((boolCell or trueCell()).value)
            assertEquals(bool, (boolCell or falseCell()).value)

            // Test `xor`.
            assertEquals(!bool, (boolCell xor trueCell()).value)
            assertEquals(bool, (boolCell xor falseCell()).value)

            // Test `!` (unary not).
            assertEquals(!bool, (!boolCell).value)
        }
    }

    @Test
    fun string_extensions() = test {
        listOf("", "  ", "\t\t", "non-empty-non-blank").forEach { string ->
            val stringCell = createWithValue(string)

            // Test the test setup first.
            assertEquals(string, stringCell.value)

            // Test `isEmpty`.
            assertEquals(string.isEmpty(), stringCell.isEmpty().value)

            // Test `isNotEmpty`.
            assertEquals(string.isNotEmpty(), stringCell.isNotEmpty().value)

            // Test `isBlank`.
            assertEquals(string.isBlank(), stringCell.isBlank().value)

            // Test `isNotBlank`.
            assertEquals(string.isNotBlank(), stringCell.isNotBlank().value)
        }
    }
}
