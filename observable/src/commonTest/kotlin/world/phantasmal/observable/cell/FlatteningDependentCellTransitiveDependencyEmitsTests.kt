package world.phantasmal.observable.cell

/**
 * In these tests the dependency of the [FlatteningDependentCell]'s direct dependency changes.
 */
class FlatteningDependentCellTransitiveDependencyEmitsTests :
    RegularCellTests,
    CellWithDependenciesTests {

    override fun createProvider() = Provider()

    override fun <T> createWithValue(value: T): FlatteningDependentCell<T> {
        val dependency = ImmutableCell(ImmutableCell(value))
        return FlatteningDependentCell(dependency) { dependency.value }
    }

    override fun createWithDependencies(
        dependency1: Cell<Int>,
        dependency2: Cell<Int>,
        dependency3: Cell<Int>,
    ) =
        FlatteningDependentCell(dependency1, dependency2, dependency3) {
            ImmutableCell(dependency1.value + dependency2.value + dependency3.value)
        }

    class Provider : CellTests.Provider {
        // The transitive dependency can change.
        private val transitiveDependency = SimpleCell(5)

        // The direct dependency of the cell under test can't change.
        private val directDependency = ImmutableCell(transitiveDependency)

        override val observable =
            FlatteningDependentCell(directDependency) { directDependency.value }

        override fun emit() {
            // Update the transitive dependency.
            transitiveDependency.value += 5
        }
    }
}
