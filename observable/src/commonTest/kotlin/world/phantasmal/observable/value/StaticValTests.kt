package world.phantasmal.observable.value

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.Scope
import world.phantasmal.testUtils.TestSuite
import kotlin.test.Test

class StaticValTests : TestSuite() {
    @Test
    fun observing_StaticVal_should_never_create_leaks() {
        val static = StaticVal("test value")

        static.observe(DummyScope) {}
        static.observe(DummyScope, callNow = false) {}
        static.observe(DummyScope, callNow = true) {}
    }

    private object DummyScope : Scope {
        override fun add(disposable: Disposable) {
            throw NotImplementedError()
        }

        override fun scope(): Scope {
            throw NotImplementedError()
        }
    }
}
