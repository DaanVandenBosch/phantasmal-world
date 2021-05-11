package world.phantasmal.observable.cell

class DependentCellTests : RegularCellTests {
    override fun createProvider() = object : CellTests.Provider {
        val dependency = SimpleCell(0)

        override val observable = DependentCell(dependency) { 2 * dependency.value }

        override fun emit() {
            dependency.value += 2
        }
    }

    override fun <T> createWithValue(value: T): DependentCell<T> {
        val dependency = SimpleCell(value)
        return DependentCell(dependency) { dependency.value }
    }
}
