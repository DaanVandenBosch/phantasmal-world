package world.phantasmal.cell.list

import world.phantasmal.cell.Cell
import world.phantasmal.cell.cell
import world.phantasmal.cell.map

// TODO: A test suite that tests FilteredListCell while all 3 types of dependencies are changing.
/**
 * Standard tests are done by [FilteredListCellListDependencyEmitsTests],
 * [FilteredListCellPredicateDependencyEmitsTests] and
 * [FilteredListCellPredicateResultDependenciesEmitTests].
 */
@Suppress("unused")
class FilteredListCellTests : AbstractFilteredListCellTests {
    override fun <E> createFilteredListCell(list: ListCell<E>, predicate: Cell<(E) -> Boolean>) =
        FilteredListCell(list, predicate.map { p -> { cell(p(it)) } })
}
