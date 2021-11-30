package world.phantasmal.observable.cell

import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.observable.test.ObservableTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals

class ImmutableCellTests : ObservableTestSuite {
    @Test
    fun observing_it_should_never_create_leaks() = test {
        val cell = ImmutableCell("test value")

        TrackedDisposable.checkNoLeaks {
            // We never call dispose on the returned disposables.
            cell.observe {}
            cell.observe(callNow = false) {}
            cell.observe(callNow = true) {}
        }
    }

    @Test
    fun observe_respects_callNow() = test {
        val cell = ImmutableCell("test value")
        var calls = 0

        cell.observe(callNow = false) { calls++ }
        cell.observe(callNow = true) { calls++ }

        assertEquals(1, calls)
    }
}
