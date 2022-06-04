package world.phantasmal.cell.test

import world.phantasmal.cell.list.ListCell
import kotlin.test.assertEquals

fun <E> assertListCellEquals(expected: List<E>, actual: ListCell<E>) {
    assertEquals(expected.size, actual.size.value)
    assertEquals(expected, actual.value)
}
