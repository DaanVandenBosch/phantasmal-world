package world.phantasmal.cell

import world.phantasmal.cell.test.CellTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals

class DoubleCellsTests : CellTestSuite {
    @Test
    fun extensions() = test {
        for (a in arrayOf(-5.0, -2.0, 0.0, 2.0, 5.0)) {
            val aCell = cell(a)

            assertEquals(-a, (-aCell).value)

            for (b in arrayOf(-5.4, -3.2, 1.138724, 2.076283, 500.0)) {
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
