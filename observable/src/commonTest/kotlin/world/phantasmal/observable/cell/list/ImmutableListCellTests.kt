package world.phantasmal.observable.cell.list

import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.observable.test.ObservableTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals

class ImmutableListCellTests : ObservableTestSuite {
    @Test
    fun observing_it_should_never_create_leaks() = test {
        val listCell = ImmutableListCell(listOf(1, 2, 3))

        TrackedDisposable.checkNoLeaks {
            // We never call dispose on the returned disposables.
            listCell.observe {}
            listCell.observe(callNow = false) {}
            listCell.observe(callNow = true) {}
            listCell.observeList(callNow = false) {}
            listCell.observeList(callNow = true) {}
        }
    }

    @Test
    fun observe_respects_callNow() = test {
        val listCell = ImmutableListCell(listOf(1, 2, 3))
        var calls = 0

        listCell.observe(callNow = false) { calls++ }
        listCell.observe(callNow = true) { calls++ }

        assertEquals(1, calls)
    }

    @Test
    fun observeList_respects_callNow() = test {
        val listCell = ImmutableListCell(listOf(1, 2, 3))
        var calls = 0

        listCell.observeList(callNow = false) { calls++ }
        listCell.observeList(callNow = true) { calls++ }

        assertEquals(1, calls)
    }
}
