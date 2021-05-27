package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.CellWithDependenciesTests
import world.phantasmal.observable.cell.StaticCell

/**
 * In these tests the dependency of the [FlatteningDependentListCell]'s direct dependency changes.
 */
class FlatteningDependentListCellTransitiveDependencyEmitsTests :
    ListCellTests,
    CellWithDependenciesTests {

    override fun createProvider() = createListProvider(empty = true)

    override fun createListProvider(empty: Boolean) = Provider(empty)

    class Provider(empty: Boolean) : ListCellTests.Provider, CellWithDependenciesTests.Provider {
        // The transitive dependency can change.
        private val transitiveDependency =
            SimpleListCell(if (empty) mutableListOf() else mutableListOf(7))

        // The direct dependency of the list under test can't change.
        private val dependency = StaticCell<ListCell<Int>>(transitiveDependency)

        override val observable =
            FlatteningDependentListCell(dependency) { dependency.value }

        override fun addElement() {
            // Update the transitive dependency.
            transitiveDependency.add(4)
        }

        override fun createWithDependencies(vararg dependencies: Cell<Int>): Cell<Any> =
            FlatteningDependentListCell(*dependencies) {
                StaticListCell(dependencies.map { it.value })
            }
    }
}
