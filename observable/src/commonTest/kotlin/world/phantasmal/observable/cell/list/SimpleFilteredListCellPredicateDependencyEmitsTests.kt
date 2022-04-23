package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.DependentCell
import world.phantasmal.observable.cell.SimpleCell

/**
 * In these tests the predicate dependency of the [SimpleListCell] changes and the list dependency
 * does not.
 */
class SimpleFilteredListCellPredicateDependencyEmitsTests : AbstractFilteredListCellTests {
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
            list = DependentListCell(dependency1, dependency2) {
                listOf(dependency1.value, dependency2.value)
            },
            predicate = DependentCell(dependency3) {
                { it < dependency3.value }
            },
        )
}
