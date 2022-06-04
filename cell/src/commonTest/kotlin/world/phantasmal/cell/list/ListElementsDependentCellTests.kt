package world.phantasmal.cell.list

import world.phantasmal.cell.ChangeEvent
import world.phantasmal.cell.SimpleCell
import world.phantasmal.cell.test.CellTestSuite
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Standard tests are done by [ListElementsDependentCellElementEmitsTests] and
 * [ListElementsDependentCellListCellEmitsTests].
 */
class ListElementsDependentCellTests : CellTestSuite {
    @Test
    fun element_changes_are_correctly_propagated() = test {
        val list = SimpleListCell(
            mutableListOf(
                SimpleCell("a"),
                SimpleCell("b"),
                SimpleCell("c")
            )
        )

        val cell = ListElementsDependentCell(list) { arrayOf(it) }

        var event: ChangeEvent<*>? = null

        disposer.add(cell.observeChange {
            assertNull(event)
            event = it
        })

        // The cell should not emit events when an old element is changed.
        run {
            val removed = list.removeAt(1)
            event = null

            removed.value += "-1"

            assertNull(event)
        }

        // The cell should emit events when any of the current elements are changed.
        list.add(SimpleCell("d"))

        for (element in list.value) {
            event = null

            element.value += "-2"

            val e = event
            assertNotNull(e)
        }
    }
}
