package world.phantasmal.cell.list

import world.phantasmal.cell.Cell
import world.phantasmal.cell.cell
import world.phantasmal.cell.map

// TODO: A test suite that tests FilteredListCell while its predicate dependency is changing.
// TODO: A test suite that tests FilteredListCell while the predicate results are changing.
// TODO: A test suite that tests FilteredListCell while all 3 types of dependencies are changing.
@Suppress("unused")
class FilteredListCellTests : SuperFilteredListCellTests {
    override fun <E> createFilteredListCell(list: ListCell<E>, predicate: Cell<(E) -> Boolean>) =
        FilteredListCell(list, predicate.map { p -> { cell(p(it)) } })
}
