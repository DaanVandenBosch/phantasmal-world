package world.phantasmal.observable.cell

/**
 * In these tests the direct dependency of the [FlatteningDependentCell] changes.
 */
class FlatteningDependentCellDirectDependencyEmitsTests : RegularCellTests {
    override fun createProvider() = object : CellTests.Provider {
        // The transitive dependency can't change.
        val transitiveDependency = ImmutableCell(5)

        // The direct dependency of the cell under test can change.
        val directDependency = SimpleCell(transitiveDependency)

        override val observable =
            FlatteningDependentCell(directDependency) { directDependency.value }

        override fun emit() {
            // Update the direct dependency.
            val oldTransitiveDependency = directDependency.value
            directDependency.value = ImmutableCell(oldTransitiveDependency.value + 5)
        }
    }

    override fun <T> createWithValue(value: T): FlatteningDependentCell<T> {
        val v = ImmutableCell(ImmutableCell(value))
        return FlatteningDependentCell(v) { v.value }
    }
}
