package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.DependentCell
import world.phantasmal.observable.cell.ImmutableCell

// TODO: A test suite that tests FilteredListCell while its predicate dependency is changing.
// TODO: A test suite that tests FilteredListCell while the predicate results are changing.
class FilteredListCellListDependencyEmitsTests : AbstractFilteredListCellTests {
    override fun createListProvider(empty: Boolean) = object : ListCellTests.Provider {
        private val dependencyCell =
            SimpleListCell(if (empty) mutableListOf(5) else mutableListOf(5, 10))

        override val observable =
            FilteredListCell(
                list = dependencyCell,
                predicate = ImmutableCell { ImmutableCell(it % 2 == 0) },
            )

        override fun addElement() {
            dependencyCell.add(4)
        }
    }

    override fun createWithDependencies(
        dependency1: Cell<Int>,
        dependency2: Cell<Int>,
        dependency3: Cell<Int>,
    ) =
        FilteredListCell(
            list = DependentListCell(dependency1, computeElements = {
                listOf(dependency1.value)
            }),
            predicate = DependentCell(dependency2, compute = {
                fun predicate(element: Int) =
                    DependentCell(dependency3, compute = { element < dependency2.value })

                ::predicate
            }),
        )
}
