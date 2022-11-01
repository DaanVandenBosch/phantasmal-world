package world.phantasmal.cell

@Suppress("unused")
class DependentCellTests : CellWithDependenciesTests {
    override fun createProvider() = Provider()

    override fun createWithDependencies(
        dependency1: Cell<Int>,
        dependency2: Cell<Int>,
        dependency3: Cell<Int>,
    ): Cell<Any> =
        DependentCell(dependency1, dependency2, dependency3) {
            dependency1.value + dependency2.value + dependency3.value
        }

    class Provider : CellTests.Provider {
        private val dependencyCell = SimpleCell(1)

        override val cell: Cell<Any> = DependentCell(dependencyCell) { 2 * dependencyCell.value }

        override fun emit() {
            dependencyCell.value += 2
        }
    }
}
