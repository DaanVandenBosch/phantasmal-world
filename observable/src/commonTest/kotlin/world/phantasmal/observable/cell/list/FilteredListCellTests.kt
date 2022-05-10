package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.cell
import world.phantasmal.observable.cell.map

// TODO: A test suite that tests FilteredListCell while its predicate dependency is changing.
// TODO: A test suite that tests FilteredListCell while the predicate results are changing.
// TODO: A test suite that tests FilteredListCell while all 3 types of dependencies are changing.
class FilteredListCellTests : SuperFilteredListCellTests {
    override fun <E> createFilteredListCell(list: ListCell<E>, predicate: Cell<(E) -> Boolean>) =
        FilteredListCell(list, predicate.map { p -> { cell(p(it)) } })
}
