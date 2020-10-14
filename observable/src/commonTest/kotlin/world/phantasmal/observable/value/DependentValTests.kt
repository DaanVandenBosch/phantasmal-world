package world.phantasmal.observable.value

import world.phantasmal.observable.observableTests
import world.phantasmal.testUtils.TestSuite
import kotlin.test.Test

class DependentValTests : TestSuite() {
    @Test
    fun observable_tests() {
        observableTests(::create)
    }

    @Test
    fun val_tests() {
        valTests(::create, ::createBoolean)
    }

    private fun create(): ValAndEmit<*> {
        val v = SimpleVal(0)
        val value = DependentVal(listOf(v)) { 2 * v.value }
        return ValAndEmit(value) { v.value += 2 }
    }

    private fun createBoolean(bool: Boolean): ValAndEmit<Boolean> {
        val v = SimpleVal(bool)
        val value = DependentVal(listOf(v)) { v.value }
        return ValAndEmit(value) { v.value = !v.value }
    }
}
