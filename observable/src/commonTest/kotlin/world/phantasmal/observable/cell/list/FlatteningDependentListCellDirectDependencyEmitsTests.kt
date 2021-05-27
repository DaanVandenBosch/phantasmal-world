package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.SimpleCell

/**
 * In these tests the direct dependency of the [FlatteningDependentListCell] changes.
 */
class FlatteningDependentListCellDirectDependencyEmitsTests : ListCellTests {
    override fun createListProvider(empty: Boolean) = object : ListCellTests.Provider {
        // The transitive dependency can't change.
        private val transitiveDependency = StaticListCell(if (empty) emptyList() else listOf(7))

        // The direct dependency of the list under test can change.
        private val dependency = SimpleCell<ListCell<Int>>(transitiveDependency)

        override val observable =
            FlatteningDependentListCell(dependency) { dependency.value }

        override fun addElement() {
            // Update the direct dependency.
            val oldTransitiveDependency: ListCell<Int> = dependency.value
            dependency.value = StaticListCell(oldTransitiveDependency.value + 4)
        }
    }
}
