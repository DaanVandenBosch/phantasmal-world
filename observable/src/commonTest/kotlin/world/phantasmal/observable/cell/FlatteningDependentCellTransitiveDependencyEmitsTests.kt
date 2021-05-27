package world.phantasmal.observable.cell

/**
 * In these tests the dependency of the [FlatteningDependentCell]'s direct dependency changes.
 */
class FlatteningDependentCellTransitiveDependencyEmitsTests :
    RegularCellTests,
    CellWithDependenciesTests {

    override fun createProvider() = Provider()

    override fun <T> createWithValue(value: T): FlatteningDependentCell<T> {
        val dependency = StaticCell(StaticCell(value))
        return FlatteningDependentCell(dependency) { dependency.value }
    }

    class Provider : CellTests.Provider, CellWithDependenciesTests.Provider {
        // The transitive dependency can change.
        private val transitiveDependency = SimpleCell(5)

        // The direct dependency of the cell under test can't change.
        private val directDependency = StaticCell(transitiveDependency)

        override val observable =
            FlatteningDependentCell(directDependency) { directDependency.value }

        override fun emit() {
            // Update the transitive dependency.
            transitiveDependency.value += 5
        }

        override fun createWithDependencies(vararg dependencies: Cell<Int>): Cell<Any> =
            FlatteningDependentCell(*dependencies) { StaticCell(dependencies.sumOf { it.value }) }
    }
}
