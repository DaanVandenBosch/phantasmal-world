package world.phantasmal.cell.test

import world.phantasmal.cell.list.ListCell
import kotlin.test.assertEquals

fun <E> assertListCellEquals(expected: List<E>, actual: ListCell<E>) {
    assertEquals(expected.size, actual.size.value)
    assertEquals(expected, actual.value)
}

/** See [snapshot]. */
typealias Snapshot = String

/**
 * We use toString to create "snapshots" of values throughout the tests. Most of the time cells will
 * actually have a new value after emitting a change event, but this is not always the case with
 * more complex cells or cells that point to complex values. So instead of keeping references to
 * values and comparing them with == (or using e.g. assertEquals), we compare snapshots.
 *
 * This of course assumes that all values have sensible toString implementations.
 */
fun Any?.snapshot(): Snapshot = toString()
