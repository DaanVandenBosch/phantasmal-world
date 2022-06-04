package world.phantasmal.cell.list

import world.phantasmal.core.disposable.DisposableTracking
import world.phantasmal.cell.observeNow
import world.phantasmal.cell.test.CellTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals

class ImmutableListCellTests : CellTestSuite {
    /**
     * As an optimization we simply ignore any observers and return a singleton Nop disposable.
     */
    @Test
    fun observing_it_never_creates_leaks() = test {
        val listCell = ImmutableListCell(listOf(1, 2, 3))

        DisposableTracking.checkNoLeaks {
            // We never call dispose on the returned disposables.
            listCell.observeChange {}
            listCell.observeListChange {}
        }
    }

    @Test
    fun observeNow_calls_the_observer_once() = test {
        val listCell = ImmutableListCell(listOf(1, 2, 3))
        var calls = 0

        listCell.observeNow { calls++ }

        assertEquals(1, calls)
    }
}
