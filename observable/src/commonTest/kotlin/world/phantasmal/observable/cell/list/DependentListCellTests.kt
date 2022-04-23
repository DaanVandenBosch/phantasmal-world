package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.CellWithDependenciesTests

class DependentListCellTests : ListCellTests, CellWithDependenciesTests {
    override fun createProvider() = createListProvider(empty = true)

    override fun createListProvider(empty: Boolean) = Provider(empty)

    override fun createWithDependencies(
        dependency1: Cell<Int>,
        dependency2: Cell<Int>,
        dependency3: Cell<Int>,
    ) =
        DependentListCell(dependency1, dependency2, dependency3) {
            listOf(dependency1.value, dependency2.value, dependency3.value)
        }

    class Provider(empty: Boolean) : ListCellTests.Provider {
        private val dependencyCell =
            SimpleListCell(if (empty) mutableListOf() else mutableListOf(5))

        override val observable =
            DependentListCell(dependencyCell) { dependencyCell.value.map { 2 * it } }

        override fun addElement() {
            dependencyCell.add(4)
        }
    }
}
