package world.phantasmal.cell.list

import world.phantasmal.cell.Cell
import world.phantasmal.cell.CellWithDependenciesTests

@Suppress("unused")
class DependentListCellTests : ListCellTests, CellWithDependenciesTests {
    override fun createProvider() = createListProvider(empty = true)

    override fun createListProvider(empty: Boolean) = Provider(empty)

    override fun createWithDependencies(
        dependency1: Cell<Int>,
        dependency2: Cell<Int>,
        dependency3: Cell<Int>,
    ): Cell<Any> =
        DependentListCell(dependency1, dependency2, dependency3) {
            listOf(dependency1.value, dependency2.value, dependency3.value)
        }

    class Provider(empty: Boolean) : ListCellTests.Provider {
        private val dependencyCell =
            SimpleListCell(if (empty) mutableListOf() else mutableListOf(5))

        override val cell: ListCell<Any> =
            DependentListCell(dependencyCell) { dependencyCell.value.map { 2 * it } }

        override fun addElement() {
            dependencyCell.add(4)
        }
    }
}
