package world.phantasmal.observable.value

import world.phantasmal.observable.test.ObservableTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals

class StaticValTests : ObservableTestSuite() {
    @Test
    fun observing_StaticVal_should_never_create_leaks() = test {
        val static = StaticVal("test value")

        static.observe {}
        static.observe(callNow = false) {}
        static.observe(callNow = true) {}
    }

    @Test
    fun observe_respects_callNow() = test {
        val static = StaticVal("test value")
        var calls = 0

        static.observe(callNow = false) { calls++ }
        static.observe(callNow = true) { calls++ }

        assertEquals(1, calls)
    }
}
