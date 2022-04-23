package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.DependentCell
import world.phantasmal.observable.cell.ImmutableCell

/**
 * In these tests the list dependency of the [SimpleListCell] changes and the predicate
 * dependency does not.
 */
class SimpleFilteredListCellListDependencyEmitsTests : AbstractFilteredListCellTests {
    override fun createListProvider(empty: Boolean) = object : ListCellTests.Provider {
        private val dependencyCell =
            SimpleListCell(if (empty) mutableListOf(5) else mutableListOf(5, 10))

        override val observable = SimpleFilteredListCell(
            list = dependencyCell,
            predicate = ImmutableCell { it % 2 == 0 },
        )

        override fun addElement() {
            dependencyCell.add(4)
        }
    }

    override fun createWithDependencies(
        dependency1: Cell<Int>,
        dependency2: Cell<Int>,
        dependency3: Cell<Int>,
    ) =
        SimpleFilteredListCell(
            list = DependentListCell(dependency1, dependency2) {
                listOf(dependency1.value, dependency2.value)
            },
            predicate = DependentCell(dependency3) {
                { it < dependency3.value }
            },
        )
}
