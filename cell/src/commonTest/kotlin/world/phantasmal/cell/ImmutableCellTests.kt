package world.phantasmal.cell

import world.phantasmal.cell.test.CellTestSuite
import world.phantasmal.core.disposable.DisposableTracking
import kotlin.test.Test
import kotlin.test.assertEquals

class ImmutableCellTests : CellTestSuite {
    /**
     * As an optimization we simply ignore any observers and return a singleton Nop disposable.
     */
    @Test
    fun observing_it_never_creates_leaks() = test {
        val cell = ImmutableCell("test value")

        DisposableTracking.checkNoLeaks {
            // We never call dispose on the returned disposable.
            cell.observeChange {}
        }
    }

    @Test
    fun observeNow_calls_the_observer_once() = test {
        val cell = ImmutableCell("test value")
        var calls = 0

        cell.observeNow { calls++ }

        assertEquals(1, calls)
    }
}
