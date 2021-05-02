package world.phantasmal.observable.cell.list

import world.phantasmal.observable.test.ObservableTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals

class StaticListCellTests : ObservableTestSuite {
    @Test
    fun observing_StaticListCell_should_never_create_leaks() = test {
        val static = StaticListCell(listOf(1, 2, 3))

        // We never call dispose on the returned disposables.
        static.observe {}
        static.observe(callNow = false) {}
        static.observe(callNow = true) {}
        static.observeList(callNow = false) {}
        static.observeList(callNow = true) {}
    }

    @Test
    fun observe_respects_callNow() = test {
        val static = StaticListCell(listOf(1, 2, 3))
        var calls = 0

        static.observe(callNow = false) { calls++ }
        static.observe(callNow = true) { calls++ }

        assertEquals(1, calls)
    }

    @Test
    fun observeList_respects_callNow() = test {
        val static = StaticListCell(listOf(1, 2, 3))
        var calls = 0

        static.observeList(callNow = false) { calls++ }
        static.observeList(callNow = true) { calls++ }

        assertEquals(1, calls)
    }
}
