package world.phantasmal.cell

import world.phantasmal.cell.test.CellTestSuite
import kotlin.test.*

class CellCreationTests : CellTestSuite {
    @Test
    fun test_cell() = test {
        assertEquals(7, cell(7).value)
    }

    @Test
    fun test_trueCell() = test {
        assertTrue(trueCell().value)
    }

    @Test
    fun test_falseCell() = test {
        assertFalse(falseCell().value)
    }

    @Test
    fun test_nullCell() = test {
        assertNull(nullCell().value)
    }

    @Test
    fun test_mutableCell_with_initial_value() = test {
        val cell = mutableCell(17)

        assertEquals(17, cell.value)

        cell.value = 201

        assertEquals(201, cell.value)
    }

    @Test
    fun test_mutableCell_with_getter_and_setter() = test {
        var x = 17
        val cell = mutableCell({ x }, { x = it })

        assertEquals(17, cell.value)

        cell.value = 201

        assertEquals(201, cell.value)
    }
}
