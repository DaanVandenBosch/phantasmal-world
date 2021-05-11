package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.StaticCell

/**
 * In these tests the dependency of the [FlatteningDependentListCell]'s direct dependency changes.
 */
class FlatteningDependentListCellTransitiveDependencyEmitsTests : ListCellTests {
    override fun createProvider() = object : ListCellTests.Provider {
        // The transitive dependency can change.
        private val transitiveDependency = SimpleListCell(mutableListOf<Int>())

        // The direct dependency of the list under test can't change.
        private val dependency = StaticCell<ListCell<Int>>(transitiveDependency)

        override val observable =
            FlatteningDependentListCell(dependency) { dependency.value }

        override fun addElement() {
            // Update the transitive dependency.
            transitiveDependency.add(4)
        }
    }
}
