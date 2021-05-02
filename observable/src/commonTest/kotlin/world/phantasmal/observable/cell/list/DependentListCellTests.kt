package world.phantasmal.observable.cell.list

class DependentListCellTests : ListCellTests {
    override fun createProvider() = object : ListCellTests.Provider {
        private val dependency = SimpleListCell<Int>(mutableListOf())

        override val observable = DependentListCell(dependency) { dependency.value.map { 2 * it } }

        override fun addElement() {
            dependency.add(4)
        }
    }
}
