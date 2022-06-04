package world.phantasmal.cell

import world.phantasmal.cell.test.CellTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals

class MutationTests : CellTestSuite {
    @Test
    fun can_change_observed_cell_with_mutateDeferred() = test {
        val cell = mutableCell(0)
        var observerCalls = 0

        disposer.add(cell.observe {
            observerCalls++

            if (it < 10) {
                mutateDeferred {
                    cell.value++
                }
            }
        })

        cell.value = 1

        assertEquals(10, observerCalls)
        assertEquals(10, cell.value)
    }
}
