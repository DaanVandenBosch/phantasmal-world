package world.phantasmal.observable.cell

class DependentCellTests : RegularCellTests, CellWithDependenciesTests {
    override fun createProvider() = Provider()

    override fun <T> createWithValue(value: T): DependentCell<T> {
        val dependency = SimpleCell(value)
        return DependentCell(dependency) { dependency.value }
    }

    class Provider : CellTests.Provider, CellWithDependenciesTests.Provider {
        private val dependency = SimpleCell(1)

        override val observable = DependentCell(dependency) { 2 * dependency.value }

        override fun emit() {
            dependency.value += 2
        }

        override fun createWithDependencies(vararg dependencies: Cell<Int>) =
            DependentCell(*dependencies) { dependencies.sumOf { it.value } }
    }
}
