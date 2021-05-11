package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.MutableCellTests
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test suite for all [MutableListCell] implementations. There is a subclass of this suite for every
 * [MutableListCell] implementation.
 */
interface MutableListCellTests<T : Any> : ListCellTests, MutableCellTests<List<T>> {
    override fun createProvider(): Provider<T>

    @Test
    fun add() = test {
        val p = createProvider()

        var change: ListChangeEvent<T>? = null

        disposer.add(p.observable.observeList {
            assertNull(change)
            change = it
        })

        // Insert once.
        val v1 = p.createElement()
        p.observable.add(v1)

        assertEquals(1, p.observable.size.value)
        assertEquals(v1, p.observable[0])
        val c1 = change
        assertTrue(c1 is ListChangeEvent.Change<T>)
        assertEquals(0, c1.index)
        assertTrue(c1.removed.isEmpty())
        assertEquals(1, c1.inserted.size)
        assertEquals(v1, c1.inserted[0])

        // Insert a second time.
        change = null

        val v2 = p.createElement()
        p.observable.add(v2)

        assertEquals(2, p.observable.size.value)
        assertEquals(v1, p.observable[0])
        assertEquals(v2, p.observable[1])
        val c2 = change
        assertTrue(c2 is ListChangeEvent.Change<T>)
        assertEquals(1, c2.index)
        assertTrue(c2.removed.isEmpty())
        assertEquals(1, c2.inserted.size)
        assertEquals(v2, c2.inserted[0])

        // Insert at index.
        change = null

        val v3 = p.createElement()
        p.observable.add(1, v3)

        assertEquals(3, p.observable.size.value)
        assertEquals(v1, p.observable[0])
        assertEquals(v3, p.observable[1])
        assertEquals(v2, p.observable[2])
        val c3 = change
        assertTrue(c3 is ListChangeEvent.Change<T>)
        assertEquals(1, c3.index)
        assertTrue(c3.removed.isEmpty())
        assertEquals(1, c3.inserted.size)
        assertEquals(v3, c3.inserted[0])
    }

    interface Provider<T : Any> : ListCellTests.Provider, MutableCellTests.Provider<List<T>> {
        override val observable: MutableListCell<T>

        fun createElement(): T
    }
}
