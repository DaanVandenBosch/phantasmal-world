package world.phantasmal.cell

import world.phantasmal.cell.test.CellTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ChangeTests : CellTestSuite {
    @Test
    fun exceptions_during_a_change_set_are_allowed() = test {
        val dependency = mutableCell(7)
        val dependent = dependency.map { 2 * it }

        var dependentObservedValue: Int? = null
        disposer.add(dependent.observeChange { dependentObservedValue = it.value })

        assertFails {
            mutate {
                dependency.value = 11
                throw Exception()
            }
        }

        // The change to dependency is still propagated because it happened before the exception.
        assertEquals(22, dependentObservedValue)
        assertEquals(22, dependent.value)

        // The machinery behind change is still in a valid state.
        mutate {
            dependency.value = 13
        }

        assertEquals(26, dependentObservedValue)
        assertEquals(26, dependent.value)
    }
}
