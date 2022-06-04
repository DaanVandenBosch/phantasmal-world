package world.phantasmal.cell.list

import world.phantasmal.cell.SimpleCell

/**
 * In these tests the direct dependency of the [FlatteningDependentListCell] changes.
 */
@Suppress("unused")
class FlatteningDependentListCellDirectDependencyEmitsTests : ListCellTests {
    override fun createListProvider(empty: Boolean) = object : ListCellTests.Provider {
        // The transitive dependency can't change.
        private val transitiveDependency = ImmutableListCell(if (empty) emptyList() else listOf(7))

        // The direct dependency of the list under test can change.
        private val directDependency = SimpleCell<ListCell<Int>>(transitiveDependency)

        override val cell =
            FlatteningDependentListCell(directDependency) { directDependency.value }

        override fun addElement() {
            // Update the direct dependency.
            val oldTransitiveDependency: ListCell<Int> = directDependency.value
            directDependency.value = ImmutableListCell(oldTransitiveDependency.value + 4)
        }
    }
}
