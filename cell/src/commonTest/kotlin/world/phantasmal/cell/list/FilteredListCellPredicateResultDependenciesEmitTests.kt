package world.phantasmal.cell.list

import world.phantasmal.cell.*

/**
 * In these tests the predicate result dependencies of the [FilteredListCell] change.
 */
@Suppress("unused")
class FilteredListCellPredicateResultDependenciesEmitTests : ListCellTests,
    CellWithDependenciesTests {
    override fun createListProvider(empty: Boolean) = object : ListCellTests.Provider {
        private var size = if (empty) 0 else 1

        // The predicate results change.
        private val predicateResultCells = (1..20).map { mutableCell(it <= size) }

        override val cell =
            FilteredListCell(
                // The list and predicate dependencies don't change.
                list = ImmutableListCell((1..20).toList()),
                predicate = cell { predicateResultCells[it - 1] },
            )

        override fun addElement() {
            predicateResultCells[size].value = true
            size++
        }
    }

    override fun createWithDependencies(
        dependency1: Cell<Int>,
        dependency2: Cell<Int>,
        dependency3: Cell<Int>,
    ): FilteredListCell<Int> {
        val deps = listOf(dependency1, dependency2, dependency3)

        return FilteredListCell(
            list = listCell(1, 2, 3),
            predicate = cell {
                deps[it - 1].map { value -> (it % 2) == (value % 2) }
            },
        )
    }
}
