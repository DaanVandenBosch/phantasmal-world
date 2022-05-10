package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.CellWithDependenciesTests
import world.phantasmal.observable.cell.SimpleCell
import world.phantasmal.observable.cell.map

/**
 * In these tests the predicate dependency of the [SimpleListCell] changes and the list dependency
 * does not.
 */
class SimpleFilteredListCellPredicateDependencyEmitsTests :
    ListCellTests, CellWithDependenciesTests {

    override fun createListProvider(empty: Boolean) = object : ListCellTests.Provider {
        private var maxValue = if (empty) 0 else 1
        private val predicateCell = SimpleCell<(Int) -> Boolean> { it <= maxValue }

        override val observable = SimpleFilteredListCell(
            list = ImmutableListCell((1..20).toList()),
            predicate = predicateCell,
        )

        override fun addElement() {
            maxValue++
            val max = maxValue
            predicateCell.value = { it <= max }
        }
    }

    override fun createWithDependencies(
        dependency1: Cell<Int>,
        dependency2: Cell<Int>,
        dependency3: Cell<Int>,
    ) =
        SimpleFilteredListCell(
            list = listCell(1, 2, 3, 4, 5, 6, 7, 8, 9),
            predicate = map(dependency1, dependency2, dependency3) { value1, value2, value3 ->
                { (it % 2) == ((value1 + value2 + value3) % 2) }
            },
        )
}
