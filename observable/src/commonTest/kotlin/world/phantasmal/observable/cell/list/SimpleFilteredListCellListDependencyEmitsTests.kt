package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.CellWithDependenciesTests
import world.phantasmal.observable.cell.cell

/**
 * In these tests the list dependency of the [SimpleListCell] changes and the predicate
 * dependency does not.
 */
class SimpleFilteredListCellListDependencyEmitsTests :
    ListCellTests, CellWithDependenciesTests {

    override fun createListProvider(empty: Boolean) = object : ListCellTests.Provider {
        private val dependencyCell =
            SimpleListCell(if (empty) mutableListOf(5) else mutableListOf(5, 10))

        override val observable = SimpleFilteredListCell(
            list = dependencyCell,
            predicate = cell { it % 2 == 0 },
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
            list = mapToList(dependency1, dependency2, dependency3) { value1, value2, value3 ->
                listOf(value1, value2, value3)
            },
            predicate = cell { it % 2 == 0 },
        )
}
