package world.phantasmal.cell.list

import world.phantasmal.cell.Cell

// TODO: A test suite that tests SimpleFilteredListCell while both types of dependencies are
//       changing.
/**
 * Standard tests are done by [SimpleFilteredListCellListDependencyEmitsTests] and
 * [SimpleFilteredListCellPredicateDependencyEmitsTests].
 */
@Suppress("unused")
class SimpleFilteredListCellTests : SuperFilteredListCellTests {
    override fun <E> createFilteredListCell(list: ListCell<E>, predicate: Cell<(E) -> Boolean>) =
        SimpleFilteredListCell(list, predicate)
}
