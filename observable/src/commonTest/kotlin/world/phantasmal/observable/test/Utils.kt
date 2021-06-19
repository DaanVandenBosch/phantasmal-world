package world.phantasmal.observable.test

import world.phantasmal.observable.cell.list.ListCell
import kotlin.test.assertEquals

fun <E> assertListCellEquals(expected: List<E>, actual: ListCell<E>) {
    assertEquals(expected.size, actual.size.value)
    assertEquals(expected.size, actual.value.size)
    assertEquals(expected, actual.value)
}
