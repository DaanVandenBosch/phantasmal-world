package world.phantasmal.cell.list

import world.phantasmal.cell.MutableCellTests
import kotlin.test.*

/**
 * Test suite for all [MutableListCell] implementations. There is a subclass of this suite for every
 * [MutableListCell] implementation.
 */
interface MutableListCellTests<T : Any> : ListCellTests, MutableCellTests<List<T>> {
    override fun createProvider(): Provider<T>

    @Test
    fun add() = test {
        val p = createProvider()

        var changeEvent: ListChangeEvent<T>? = null

        disposer.add(p.cell.observeListChange {
            assertNull(changeEvent)
            changeEvent = it
        })

        // Insert once.
        val v1 = p.createElement()
        p.cell.add(v1)

        run {
            assertEquals(1, p.cell.size.value)
            assertEquals(v1, p.cell[0])

            val e = changeEvent
            assertNotNull(e)
            assertEquals(1, e.changes.size)

            val c0 = e.changes[0]
            assertEquals(0, c0.index)
            assertTrue(c0.removed.isEmpty())
            assertEquals(1, c0.inserted.size)
            assertEquals(v1, c0.inserted[0])
        }

        // Insert a second time.
        changeEvent = null

        val v2 = p.createElement()
        p.cell.add(v2)

        run {
            assertEquals(2, p.cell.size.value)
            assertEquals(v1, p.cell[0])
            assertEquals(v2, p.cell[1])

            val e = changeEvent
            assertNotNull(e)
            assertEquals(1, e.changes.size)

            val c0 = e.changes[0]
            assertEquals(1, c0.index)
            assertTrue(c0.removed.isEmpty())
            assertEquals(1, c0.inserted.size)
            assertEquals(v2, c0.inserted[0])
        }

        // Insert at index.
        changeEvent = null

        val v3 = p.createElement()
        p.cell.add(1, v3)

        run {
            assertEquals(3, p.cell.size.value)
            assertEquals(v1, p.cell[0])
            assertEquals(v3, p.cell[1])
            assertEquals(v2, p.cell[2])

            val e = changeEvent
            assertNotNull(e)
            assertEquals(1, e.changes.size)

            val c0 = e.changes[0]
            assertEquals(1, c0.index)
            assertTrue(c0.removed.isEmpty())
            assertEquals(1, c0.inserted.size)
            assertEquals(v3, c0.inserted[0])
        }
    }

    interface Provider<T : Any> : ListCellTests.Provider, MutableCellTests.Provider<List<T>> {
        override val cell: MutableListCell<T>

        fun createElement(): T
    }
}
