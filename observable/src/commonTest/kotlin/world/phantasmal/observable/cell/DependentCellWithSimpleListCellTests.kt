package world.phantasmal.observable.cell

import world.phantasmal.observable.cell.list.SimpleListCell

class DependentCellWithSimpleListCellTests : CellTests {
    override fun createProvider() = Provider()

    class Provider : CellTests.Provider {
        private val dependencyCell = SimpleListCell(mutableListOf("a", "b", "c"))

        override val observable = DependentCell(dependencyCell) { dependencyCell.value }

        override fun emit() {
            dependencyCell.add("x")
        }
    }
}
