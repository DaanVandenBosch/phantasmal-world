package world.phantasmal.cell

import world.phantasmal.cell.test.CellTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals

class IntCellsTests : CellTestSuite {
    @Test
    fun extensions() = test {
        for (a in -5..5) {
            val aCell = cell(a)

            assertEquals(-a, (-aCell).value)

            for (b in 1..5) {
                val bCell = cell(b)

                assertEquals(a + b, (aCell + bCell).value)
                assertEquals(a + b, (aCell + b).value)
                assertEquals(a + b, (a + bCell).value)

                assertEquals(a - b, (aCell - bCell).value)
                assertEquals(a - b, (aCell - b).value)
                assertEquals(a - b, (a - bCell).value)

                assertEquals(a * b, (aCell * bCell).value)
                assertEquals(a * b, (aCell * b).value)
                assertEquals(a * b, (a * bCell).value)

                assertEquals(a / b, (aCell / bCell).value)
                assertEquals(a / b, (aCell / b).value)
                assertEquals(a / b, (a / bCell).value)
            }
        }
    }
}
