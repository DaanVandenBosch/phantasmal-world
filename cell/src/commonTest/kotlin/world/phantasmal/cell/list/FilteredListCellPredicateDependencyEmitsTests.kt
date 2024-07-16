package world.phantasmal.cell.list

import world.phantasmal.cell.*

/**
 * In these tests the predicate dependency of the [FilteredListCell] changes.
 */
@Suppress("unused")
class FilteredListCellPredicateDependencyEmitsTests : ListCellTests, CellWithDependenciesTests {
    override fun createListProvider(empty: Boolean) = object : ListCellTests.Provider {
        private var maxValue = if (empty) 0 else 1

        // The predicate cell changes, the predicate results don't.
        private val predicateCell: SimpleCell<(Int) -> Cell<Boolean>> =
            SimpleCell { cell(it <= maxValue) }

        override val cell =
            FilteredListCell(
                // The list dependency doesn't change.
                list = ImmutableListCell((1..20).toList()),
                predicate = predicateCell,
            )

        override fun addElement() {
            maxValue++
            val max = maxValue
            predicateCell.value = { cell(it <= max) }
        }
    }

    override fun createWithDependencies(
        dependency1: Cell<Int>,
        dependency2: Cell<Int>,
        dependency3: Cell<Int>,
    ): Cell<Any> =
        FilteredListCell(
            list = listCell(1, 2, 3, 4, 5, 6, 7, 8, 9),
            predicate = map(dependency1, dependency2, dependency3) { value1, value2, value3 ->
                { cell((it % 2) == ((value1 + value2 + value3) % 2)) }
            },
        )
}
