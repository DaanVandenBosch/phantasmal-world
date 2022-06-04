package world.phantasmal.cell

/**
 * In these tests both the direct dependency and the transitive dependency of the
 * [FlatteningDependentCell] change.
 */
@Suppress("unused")
class FlatteningDependentCellDirectAndTransitiveDependencyEmitTests : CellTests {
    override fun createProvider() = Provider()

    class Provider : CellTests.Provider {
        // This cell is both the direct and transitive dependency.
        private val dependencyCell = SimpleCell('a')

        override val cell = FlatteningDependentCell(dependencyCell) { dependencyCell }

        override fun emit() {
            dependencyCell.value += 1
        }
    }
}
