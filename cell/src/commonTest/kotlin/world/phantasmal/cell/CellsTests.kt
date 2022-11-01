package world.phantasmal.cell

import world.phantasmal.cell.test.CellTestSuite
import kotlin.test.*

class CellsTests : CellTestSuite {
    @Test
    fun cell_contains_the_given_value() = test {
        val c: Cell<String> = cell("test_value")
        assertEquals("test_value", c.value)
    }

    @Test
    fun trueCell_is_always_true() = test {
        val c: Cell<Boolean> = trueCell()
        assertEquals(true, c.value)
    }

    @Test
    fun falseCell_is_always_false() = test {
        val c: Cell<Boolean> = falseCell()
        assertEquals(false, c.value)
    }

    @Test
    fun nullCell_is_always_null() = test {
        val c: Cell<Nothing?> = nullCell()
        assertNull(c.value)
    }

    @Test
    fun zeroIntCell_is_always_zero() = test {
        val c: Cell<Int> = zeroIntCell()
        assertEquals(0, c.value)
    }

    @Test
    fun emptyStringCell_is_always_empty() = test {
        val c: Cell<String> = emptyStringCell()
        assertEquals("", c.value)
    }

    @Test
    fun mutableCell_contains_the_given_value() = test {
        val c: MutableCell<String> = mutableCell("test_value")
        assertEquals("test_value", c.value)
    }

    @Test
    fun isNull() = test {
        for (value in arrayOf(Any(), null)) {
            val c = cell(value).isNull()

            assertEquals(value == null, c.value)
        }
    }

    @Test
    fun isNotNull() = test {
        for (value in arrayOf(Any(), null)) {
            val c = cell(value).isNotNull()

            assertEquals(value != null, c.value)
        }
    }

    @Test
    fun equality_infix_functions() = test {
        val a = cell("equal")
        val b = cell("equal")
        val c = cell("NOT equal")

        assertTrue((a eq b).value)
        assertTrue((a eq "equal").value)
        assertFalse((a eq c).value)
        assertFalse((a eq "NOT equal").value)

        assertFalse((a ne b).value)
        assertFalse((a ne "equal").value)
        assertTrue((a ne c).value)
        assertTrue((a ne "NOT equal").value)
    }

    @Test
    fun orElse() = test {
        for (value in arrayOf("value", null)) {
            val c: Cell<String?> = cell(value)
            val coe: Cell<String> = c.orElse { "default" }

            assertEquals(value ?: "default", coe.value)
        }
    }

    @Test
    fun comparable_extensions() = test {
        val a = cell(1)
        val b = cell(2)

        assertFalse((a gt b).value)
        assertFalse((a gt 2).value)
        assertFalse((1 gt b).value)

        assertTrue((a lt b).value)
        assertTrue((a lt 2).value)
        assertTrue((1 lt b).value)
    }

    @Test
    fun boolean_extensions() = test {
        for (a in arrayOf(false, true)) {
            val aCell = cell(a)

            assertEquals(!a, (!aCell).value)

            for (b in arrayOf(false, true)) {
                val bCell = cell(b)

                assertEquals(a && b, (aCell and bCell).value)
                assertEquals(a && b, (aCell and b).value)
                assertEquals(a && b, (a and bCell).value)

                assertEquals(a || b, (aCell or bCell).value)
                assertEquals(a || b, (aCell or b).value)
                assertEquals(a || b, (a or bCell).value)

                // Use != because of https://youtrack.jetbrains.com/issue/KT-31277.
                assertEquals(a != b, (aCell xor bCell).value)
                assertEquals(a != b, (aCell xor b).value)
                assertEquals(a != b, (a xor bCell).value)
            }
        }
    }

    @Test
    fun string_extensions() = test {
        for (string in arrayOf("", "  ", "\t\t", "non-empty-non-blank")) {
            val stringCell = cell(string)

            assertEquals(string.isEmpty(), stringCell.isEmpty().value)

            assertEquals(string.isNotEmpty(), stringCell.isNotEmpty().value)

            assertEquals(string.isBlank(), stringCell.isBlank().value)

            assertEquals(string.isNotBlank(), stringCell.isNotBlank().value)
        }
    }
}
