package world.phantasmal.observable.value.list

import world.phantasmal.observable.test.ObservableTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals

class StaticListValTests : ObservableTestSuite() {
    @Test
    fun observing_StaticListVal_should_never_create_leaks() = test {
        val static = StaticListVal(listOf(1, 2, 3))

        static.observe {}
        static.observe(callNow = false) {}
        static.observe(callNow = true) {}
        static.observeList(callNow = false) {}
        static.observeList(callNow = true) {}
    }

    @Test
    fun observe_respects_callNow() = test {
        val static = StaticListVal(listOf(1, 2, 3))
        var calls = 0

        static.observe(callNow = false) { calls++ }
        static.observe(callNow = true) { calls++ }

        assertEquals(1, calls)
    }

    @Test
    fun observeList_respects_callNow() = test {
        val static = StaticListVal(listOf(1, 2, 3))
        var calls = 0

        static.observeList(callNow = false) { calls++ }
        static.observeList(callNow = true) { calls++ }

        assertEquals(1, calls)
    }
}
