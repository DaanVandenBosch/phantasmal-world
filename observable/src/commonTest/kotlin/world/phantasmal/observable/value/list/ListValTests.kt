package world.phantasmal.observable.value.list

import world.phantasmal.observable.value.ValAndEmit
import world.phantasmal.observable.value.ValTests
import kotlin.test.*

interface ListValAndAdd : ValAndEmit {
    override val observable: ListVal<Any>

    fun add()

    override fun emit() = add()

    override operator fun component1() = observable
}

/**
 * Test suite for all [ListVal] implementations. There is a subclass of this suite for every
 * [ListVal] implementation.
 */
abstract class ListValTests : ValTests() {
    abstract override fun create(): ListValAndAdd

    @Test
    fun calls_list_observers_when_changed() = test {
        val (list, add) = create()

        var event: ListValChangeEvent<*>? = null

        disposer.add(
            list.observeList {
                assertNull(event)
                event = it
            }
        )

        for (i in 0..2) {
            event = null

            add()

            assertTrue(event is ListValChangeEvent.Change<*>)
        }
    }

    @Test
    fun updates_size_correctly() = test {
        val (list, add) = create()

        assertEquals(0, list.size.value)

        var observedSize: Int? = null

        disposer.add(
            list.size.observe {
                assertNull(observedSize)
                observedSize = it.value
            }
        )

        for (i in 1..3) {
            observedSize = null

            add()

            assertEquals(i, list.size.value)
            assertEquals(i, observedSize)
        }
    }

    @Test
    fun get() = test {
        val (list, add) = create()

        assertFailsWith(IndexOutOfBoundsException::class) {
            list[0]
        }

        add()

        // Shouldn't throw at this point.
        list[0]
    }

    @Test
    fun fold() = test {
        val (list, add) = create()

        val fold = list.fold(0) { acc, _ -> acc + 1 }

        var observedValue: Int? = null

        disposer.add(fold.observe {
            assertNull(observedValue)
            observedValue = it.value
        })

        assertEquals(0, fold.value)

        for (i in 1..5) {
            observedValue = null

            add()

            assertEquals(i, fold.value)
            assertEquals(i, observedValue)
        }
    }

    @Test
    fun sumBy() = test {
        val (list, add) = create()

        val sum = list.sumBy { 1 }

        var observedValue: Int? = null

        disposer.add(sum.observe {
            assertNull(observedValue)
            observedValue = it.value
        })

        assertEquals(0, sum.value)

        for (i in 1..5) {
            observedValue = null

            add()

            assertEquals(i, sum.value)
            assertEquals(i, observedValue)
        }
    }

    @Test
    fun filtered() = test {
        val (list, add) = create()

        val filtered = list.filtered { true }

        var event: ListValChangeEvent<*>? = null

        disposer.add(filtered.observeList {
            assertNull(event)
            event = it
        })

        assertEquals(0, filtered.size.value)

        for (i in 1..5) {
            event = null

            add()

            assertEquals(i, filtered.size.value)
            assertNotNull(event)
        }
    }

    @Test
    fun firstOrNull() = test {
        val (list, add) = create()

        val firstOrNull = list.firstOrNull()

        var observedValue: Any? = null

        disposer.add(firstOrNull.observe {
            assertNull(observedValue)
            observedValue = it.value
        })

        assertNull(firstOrNull.value)

        add()

        assertNotNull(firstOrNull.value)
        assertNotNull(observedValue)

        repeat(3) {
            observedValue = null

            add()

            assertNotNull(firstOrNull.value)
            // Observer should not be called when adding elements at the end of the list.
            assertNull(observedValue)
        }
    }
}
