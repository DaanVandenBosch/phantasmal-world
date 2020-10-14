package world.phantasmal.observable.value

import world.phantasmal.observable.observableTests
import world.phantasmal.testUtils.TestSuite
import kotlin.test.Test

class SimpleValTests : TestSuite() {
    @Test
    fun observable_tests() {
        observableTests(::create)
    }

    @Test
    fun val_tests() {
        valTests(::create, ::createBoolean)
    }

    private fun create(): ValAndEmit<*> {
        val value = SimpleVal(1)
        return ValAndEmit(value) { value.value += 2 }
    }

    private fun createBoolean(bool: Boolean): ValAndEmit<Boolean> {
        val value = SimpleVal(bool)
        return ValAndEmit(value) { value.value = !value.value }
    }
}
