package world.phantasmal.observable.value

import world.phantasmal.testUtils.TestSuite
import kotlin.test.Test

class StaticValTests : TestSuite() {
    @Test
    fun observing_StaticVal_should_never_create_leaks() = test {
        val static = StaticVal("test value")

        static.observe {}
        static.observe(callNow = false) {}
        static.observe(callNow = true) {}
    }
}
