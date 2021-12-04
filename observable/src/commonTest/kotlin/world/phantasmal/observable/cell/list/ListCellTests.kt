package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.CellTests
import kotlin.test.*

/**
 * Test suite for all [ListCell] implementations. There is a subclass of this suite for every
 * [ListCell] implementation.
 */
interface ListCellTests : CellTests {
    override fun createProvider(): Provider = createListProvider(empty = true)

    fun createListProvider(empty: Boolean): Provider

    @Test
    fun list_value_is_accessible_without_observers() = test {
        val p = createListProvider(empty = false)

        // We literally just test that accessing the value property doesn't throw or return the
        // wrong list.
        assertTrue(p.observable.value.isNotEmpty())
    }

    @Test
    fun list_value_is_accessible_with_observers() = test {
        val p = createListProvider(empty = false)

        disposer.add(p.observable.observeListChange {})

        // We literally just test that accessing the value property doesn't throw or return the
        // wrong list.
        assertTrue(p.observable.value.isNotEmpty())
    }

    @Test
    fun emits_no_list_change_event_until_changed() = test {
        val p = createListProvider(empty = false)

        var observedEvent: ListChangeEvent<Any>? = null

        disposer.add(p.observable.observeListChange { listChangeEvent ->
            observedEvent = listChangeEvent
        })

        assertNull(observedEvent)

        p.emit()

        assertNotNull(observedEvent)
    }

    @Test
    fun calls_list_observers_when_changed() = test {
        val p = createProvider()

        var event: ListChangeEvent<*>? = null

        disposer.add(
            p.observable.observeListChange {
                assertNull(event)
                event = it
            }
        )

        for (i in 0..2) {
            event = null

            p.addElement()

            assertNotNull(event)
        }
    }

    @Test
    fun updates_size_correctly() = test {
        val p = createProvider()

        assertEquals(0, p.observable.size.value)

        var observedSize: Int? = null

        disposer.add(
            p.observable.size.observeChange {
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

        disposer.add(fold.observeChange {
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

        val sum = p.observable.sumOf { 1 }

        var observedValue: Int? = null

        disposer.add(sum.observeChange {
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

        disposer.add(filtered.observeListChange {
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

        disposer.add(firstOrNull.observeChange {
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
