package world.phantasmal.observable.cell

class DependentCellTests : RegularCellTests, CellWithDependenciesTests {
    override fun createProvider() = Provider()

    override fun <T> createWithValue(value: T): DependentCell<T> {
        val dependency = SimpleCell(value)
        return DependentCell(dependency) { dependency.value }
    }

    override fun createWithDependencies(
        dependency1: Cell<Int>,
        dependency2: Cell<Int>,
        dependency3: Cell<Int>,
    ) =
        DependentCell(dependency1, dependency2, dependency3) {
            dependency1.value + dependency2.value + dependency3.value
        }

    class Provider : CellTests.Provider {
        private val dependencyCell = SimpleCell(1)

        override val observable = DependentCell(dependencyCell) { 2 * dependencyCell.value }

        override fun emit() {
            dependencyCell.value += 2
        }
    }
}
