package world.phantasmal.cell

import world.phantasmal.cell.list.SimpleListCell

@Suppress("unused")
class DependentCellWithSimpleListCellTests : CellTests {
    override fun createProvider() = Provider()

    class Provider : CellTests.Provider {
        private val dependencyCell = SimpleListCell(mutableListOf("a", "b", "c"))

        override val cell = DependentCell(dependencyCell) { dependencyCell.value }

        override fun emit() {
            dependencyCell.add("x")
        }
    }
}
