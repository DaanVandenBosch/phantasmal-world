package world.phantasmal.cell.list

import world.phantasmal.cell.SimpleCell
import world.phantasmal.cell.test.CellTestSuite
import world.phantasmal.cell.test.assertListCellEquals
import kotlin.test.Test

class ListCellCreationTests : CellTestSuite {
    @Test
    fun test_flatMapToList() = test {
        val cell = SimpleCell(SimpleListCell(mutableListOf(1, 2, 3, 4, 5)))

        val mapped = cell.flatMapToList { it }

        assertListCellEquals(listOf(1, 2, 3, 4, 5), mapped)
    }
}
