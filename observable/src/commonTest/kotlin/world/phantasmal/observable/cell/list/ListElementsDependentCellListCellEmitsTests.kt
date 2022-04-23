package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.CellTests
import world.phantasmal.observable.cell.ImmutableCell

/**
 * In these tests, the direct list cell dependency of the [ListElementsDependentCell] changes, while
 * its elements don't change.
 */
class ListElementsDependentCellListCellEmitsTests : CellTests {

    override fun createProvider() = object : CellTests.Provider {
        // The direct dependency of the list under test changes, its elements are immutable.
        private val directDependency: SimpleListCell<Cell<Int>> =
            SimpleListCell(mutableListOf(ImmutableCell(1), ImmutableCell(2), ImmutableCell(3)))

        override val observable =
            ListElementsDependentCell(directDependency) { arrayOf(it) }

        override fun emit() {
            directDependency.add(ImmutableCell(directDependency.value.size + 1))
        }
    }
}
