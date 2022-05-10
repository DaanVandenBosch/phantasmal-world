package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.Cell

// TODO: A test suite that tests SimpleFilteredListCell while both types of dependencies are
//       changing.
class SimpleFilteredListCellTests : SuperFilteredListCellTests {
    override fun <E> createFilteredListCell(list: ListCell<E>, predicate: Cell<(E) -> Boolean>) =
        SimpleFilteredListCell(list, predicate)
}
