package world.phantasmal.observable.cell

import world.phantasmal.observable.cell.list.SimpleListCell

class FlatteningDependentCellWithSimpleListCellTests : CellTests {
    override fun createProvider() = Provider()

    class Provider : CellTests.Provider {
        private val dependencyCell = SimpleListCell(mutableListOf("a", "b", "c"))

        override val observable = FlatteningDependentCell(dependencyCell) { dependencyCell }

        override fun emit() {
            dependencyCell.add("x")
        }
    }
}
