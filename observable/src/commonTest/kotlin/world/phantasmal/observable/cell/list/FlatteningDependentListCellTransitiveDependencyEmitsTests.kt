package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.CellTests
import world.phantasmal.observable.cell.CellWithDependenciesTests
import world.phantasmal.observable.cell.ImmutableCell

/**
 * In these tests the dependency of the [FlatteningDependentListCell]'s direct dependency changes.
 */
class FlatteningDependentListCellTransitiveDependencyEmitsTests :
    ListCellTests,
    CellWithDependenciesTests {

    override fun createProvider() = createListProvider(empty = true)

    override fun createListProvider(empty: Boolean) = Provider(empty)

    override fun createWithDependencies(
        dependency1: Cell<Int>,
        dependency2: Cell<Int>,
        dependency3: Cell<Int>,
    ) =
        FlatteningDependentListCell(dependency1, dependency2, dependency3) {
            ImmutableListCell(listOf(dependency1.value, dependency2.value, dependency3.value))
        }

    class Provider(empty: Boolean) : ListCellTests.Provider, CellTests.Provider {
        // The transitive dependency can change.
        private val transitiveDependency =
            SimpleListCell(if (empty) mutableListOf() else mutableListOf(7))

        // The direct dependency of the list under test can't change.
        private val directDependency = ImmutableCell<ListCell<Int>>(transitiveDependency)

        override val observable =
            FlatteningDependentListCell(directDependency) { directDependency.value }

        override fun addElement() {
            // Update the transitive dependency.
            transitiveDependency.add(4)
        }
    }
}
