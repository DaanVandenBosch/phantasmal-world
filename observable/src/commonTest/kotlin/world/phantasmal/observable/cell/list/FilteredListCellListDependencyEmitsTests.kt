package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.*

class FilteredListCellListDependencyEmitsTests : ListCellTests, CellWithDependenciesTests {
    override fun createListProvider(empty: Boolean) = object : ListCellTests.Provider {
        private val dependencyCell =
            SimpleListCell(if (empty) mutableListOf(5) else mutableListOf(5, 10))

        override val observable =
            FilteredListCell(
                list = dependencyCell,
                predicate = cell { cell(it % 2 == 0) },
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
            list = dependency1.mapToList { listOf(it) },
            predicate = dependency2.map { value2 ->
                fun predicate(element: Int): Cell<Boolean> =
                    dependency3.map { value3 -> (element % 2) == ((value2 + value3) % 2) }

                ::predicate
            },
        )
}
