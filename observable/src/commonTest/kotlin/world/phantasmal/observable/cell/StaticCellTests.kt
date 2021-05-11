package world.phantasmal.observable.cell

import world.phantasmal.observable.test.ObservableTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals

class StaticCellTests : ObservableTestSuite {
    @Test
    fun observing_StaticCell_should_never_create_leaks() = test {
        val static = StaticCell("test value")

        // We never call dispose on the returned disposables.
        static.observe {}
        static.observe(callNow = false) {}
        static.observe(callNow = true) {}
    }

    @Test
    fun observe_respects_callNow() = test {
        val static = StaticCell("test value")
        var calls = 0

        static.observe(callNow = false) { calls++ }
        static.observe(callNow = true) { calls++ }

        assertEquals(1, calls)
    }
}
