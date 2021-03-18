package world.phantasmal.observable.value.list

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

interface MutableListValAndAdd : ListValAndAdd {
    override val observable: MutableListVal<Int>

    override operator fun component1() = observable
}

/**
 * Test suite for all [MutableListVal] implementations. There is a subclass of this suite for every
 * [MutableListVal] implementation.
 */
abstract class MutableListValTests : ListValTests() {
    abstract override fun create(): MutableListValAndAdd

    @Test
    fun add() = test {
        val (list: MutableListVal<Int>) = create()

        var change: ListValChangeEvent<Int>? = null

        disposer.add(list.observeList {
            assertNull(change)
            change = it
        })

        // Insert once.
        list.add(7)

        assertEquals(1, list.size.value)
        assertEquals(7, list[0])
        val c1 = change
        assertTrue(c1 is ListValChangeEvent.Change<Int>)
        assertEquals(0, c1.index)
        assertTrue(c1.removed.isEmpty())
        assertEquals(1, c1.inserted.size)
        assertEquals(7, c1.inserted[0])

        // Insert a second time.
        change = null

        list.add(11)

        assertEquals(2, list.size.value)
        assertEquals(7, list[0])
        assertEquals(11, list[1])
        val c2 = change
        assertTrue(c2 is ListValChangeEvent.Change<Int>)
        assertEquals(1, c2.index)
        assertTrue(c2.removed.isEmpty())
        assertEquals(1, c2.inserted.size)
        assertEquals(11, c2.inserted[0])

        // Insert at index.
        change = null

        list.add(1, 13)

        assertEquals(3, list.size.value)
        assertEquals(7, list[0])
        assertEquals(13, list[1])
        assertEquals(11, list[2])
        val c3 = change
        assertTrue(c3 is ListValChangeEvent.Change<Int>)
        assertEquals(1, c3.index)
        assertTrue(c3.removed.isEmpty())
        assertEquals(1, c3.inserted.size)
        assertEquals(13, c3.inserted[0])
    }
}
