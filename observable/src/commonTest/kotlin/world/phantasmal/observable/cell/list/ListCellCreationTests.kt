package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.SimpleCell
import world.phantasmal.observable.test.ObservableTestSuite
import world.phantasmal.observable.test.assertListCellEquals
import kotlin.test.Test

class ListCellCreationTests : ObservableTestSuite {
    @Test
    fun test_flatMapToList() = test {
        val cell = SimpleCell(SimpleListCell(mutableListOf(1, 2, 3, 4, 5)))

        val mapped = cell.flatMapToList { it }

        assertListCellEquals(listOf(1, 2, 3, 4, 5), mapped)
    }
}
