package world.phantasmal.observable.value.list

import world.phantasmal.observable.observableTests
import world.phantasmal.observable.test.TestSuite
import world.phantasmal.observable.value.valTests
import kotlin.test.Test

class SimpleListValTests : TestSuite() {
    @Test
    fun observable_tests() {
        observableTests(::create)
    }

    @Test
    fun val_tests() {
        valTests(::create, createBoolean = null)
    }

    @Test
    fun list_val_tests() {
        listValTests(::create)
    }

    private fun create(): ListValAndAdd {
        val value = SimpleListVal(mutableListOf<Int>())
        return ListValAndAdd(value) { value.add(7) }
    }
}
