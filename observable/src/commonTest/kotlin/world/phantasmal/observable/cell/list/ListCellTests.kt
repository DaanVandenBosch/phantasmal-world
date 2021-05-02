package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.CellTests
import kotlin.test.*

/**
 * Test suite for all [ListCell] implementations. There is a subclass of this suite for every
 * [ListCell] implementation.
 */
interface ListCellTests : CellTests {
    override fun createProvider(): Provider

    @Test
    fun calls_list_observers_when_changed() = test {
        val p = createProvider()

        var event: ListChangeEvent<*>? = null

        disposer.add(
            p.observable.observeList {
                assertNull(event)
                event = it
            }
        )

        for (i in 0..2) {
            event = null

            p.addElement()

            assertTrue(event is ListChangeEvent.Change<*>)
        }
    }

    @Test
    fun updates_size_correctly() = test {
        val p = createProvider()

        assertEquals(0, p.observable.size.value)

        var observedSize: Int? = null

        disposer.add(
            p.observable.size.observe {
                assertNull(observedSize)
                observedSize = it.value
            }
        )

        for (i in 1..3) {
            observedSize = null

            p.addElement()

            assertEquals(i, p.observable.size.value)
            assertEquals(i, observedSize)
        }
    }

    @Test
    fun get() = test {
        val p = createProvider()

        assertFailsWith(IndexOutOfBoundsException::class) {
            p.observable[0]
        }

        p.addElement()

        // Shouldn't throw at this point.
        p.observable[0]
    }

    @Test
    fun fold() = test {
        val p = createProvider()

        val fold = p.observable.fold(0) { acc, _ -> acc + 1 }

        var observedValue: Int? = null

        disposer.add(fold.observe {
            assertNull(observedValue)
            observedValue = it.value
        })

        assertEquals(0, fold.value)

        for (i in 1..5) {
            observedValue = null

            p.addElement()

            assertEquals(i, fold.value)
            assertEquals(i, observedValue)
        }
    }

    @Test
    fun sumBy() = test {
        val p = createProvider()

        val sum = p.observable.sumBy { 1 }

        var observedValue: Int? = null

        disposer.add(sum.observe {
            assertNull(observedValue)
            observedValue = it.value
        })

        assertEquals(0, sum.value)

        for (i in 1..5) {
            observedValue = null

            p.addElement()

            assertEquals(i, sum.value)
            assertEquals(i, observedValue)
        }
    }

    @Test
    fun filtered() = test {
        val p = createProvider()

        val filtered = p.observable.filtered { true }

        var event: ListChangeEvent<*>? = null

        disposer.add(filtered.observeList {
            assertNull(event)
            event = it
        })

        assertEquals(0, filtered.size.value)

        for (i in 1..5) {
            event = null

            p.addElement()

            assertEquals(i, filtered.size.value)
            assertNotNull(event)
        }
    }

    @Test
    fun firstOrNull() = test {
        val p = createProvider()

        val firstOrNull = p.observable.firstOrNull()

        var observedValue: Any? = null

        disposer.add(firstOrNull.observe {
            assertNull(observedValue)
            observedValue = it.value
        })

        assertNull(firstOrNull.value)

        p.addElement()

        assertNotNull(firstOrNull.value)
        assertNotNull(observedValue)

        repeat(3) {
            observedValue = null

            p.addElement()

            assertNotNull(firstOrNull.value)
            // Observer should not be called when adding elements at the end of the list.
            assertNull(observedValue)
        }
    }

    interface Provider : CellTests.Provider {
        override val observable: ListCell<Any>

        /**
         * Adds an element to the [ListCell] under test.
         */
        fun addElement()

        override fun emit() = addElement()
    }
}
