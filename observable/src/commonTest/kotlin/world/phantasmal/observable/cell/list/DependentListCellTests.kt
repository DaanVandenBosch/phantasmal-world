package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.CellWithDependenciesTests

class DependentListCellTests : ListCellTests, CellWithDependenciesTests {
    override fun createProvider() = createListProvider(empty = true)

    override fun createListProvider(empty: Boolean) = Provider(empty)

    class Provider(empty: Boolean) : ListCellTests.Provider, CellWithDependenciesTests.Provider {
        private val dependency = SimpleListCell(if (empty) mutableListOf() else mutableListOf(5))

        override val observable = DependentListCell(dependency) { dependency.value.map { 2 * it } }

        override fun addElement() {
            dependency.add(4)
        }

        override fun createWithDependencies(vararg dependencies: Cell<Int>): Cell<Any> =
            DependentListCell(*dependencies) { dependencies.map { it.value } }
    }
}
