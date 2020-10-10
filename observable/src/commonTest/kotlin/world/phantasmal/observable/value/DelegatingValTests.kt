package world.phantasmal.observable.value

import world.phantasmal.observable.observableTests
import world.phantasmal.observable.test.TestSuite
import kotlin.test.Test

class DelegatingValTests : TestSuite() {
    @Test
    fun observable_tests() {
        observableTests(::create)
    }

    @Test
    fun val_tests() {
        valTests(::create, ::createBoolean)
    }

    private fun create(): ValAndEmit<*> {
        var v = 0
        val value = DelegatingVal({ v }, { v = it })
        return ValAndEmit(value) { value.value += 2 }
    }

    private fun createBoolean(bool: Boolean): ValAndEmit<Boolean> {
        var v = bool
        val value = DelegatingVal({ v }, { v = it })
        return ValAndEmit(value) { value.value = !value.value }
    }
}
