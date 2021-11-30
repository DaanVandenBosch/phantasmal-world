package world.phantasmal.observable.cell

import world.phantasmal.core.disposable.use
import world.phantasmal.observable.ObservableTests
import kotlin.test.*

/**
 * Test suite for all [Cell] implementations. There is a subclass of this suite for every [Cell]
 * implementation.
 */
interface CellTests : ObservableTests {
    override fun createProvider(): Provider

    @Test
    fun value_is_accessible_without_observers() = test {
        val p = createProvider()

        assertNotNull(p.observable.value)
    }

    @Test
    fun value_is_accessible_with_observers() = test {
        val p = createProvider()

        var observedValue: Any? = null

        disposer.add(p.observable.observe(callNow = true) {
            observedValue = it.value
        })

        assertNotNull(observedValue)
        assertNotNull(p.observable.value)
    }

    @Test
    fun propagates_changes_to_mapped_cell() = test {
        val p = createProvider()
        val mapped = p.observable.map { it.hashCode() }
        val initialValue = mapped.value

        var observedValue: Int? = null

        disposer.add(mapped.observe {
            assertNull(observedValue)
            observedValue = it.value
        })

        p.emit()

        assertNotEquals(initialValue, mapped.value)
        assertEquals(mapped.value, observedValue)
    }

    @Test
    fun propagates_changes_to_flat_mapped_cell() = test {
        val p = createProvider()

        val mapped = p.observable.flatMap { ImmutableCell(it.hashCode()) }
        val initialValue = mapped.value

        var observedValue: Int? = null

        disposer.add(mapped.observe {
            assertNull(observedValue)
            observedValue = it.value
        })

        p.emit()

        assertNotEquals(initialValue, mapped.value)
        assertEquals(mapped.value, observedValue)
    }

    @Test
    fun emits_correct_value_in_change_events() = test {
        val p = createProvider()

        var observedValue: Any? = null

        disposer.add(p.observable.observe {
            assertNull(observedValue)
            observedValue = it.value
        })

        repeat(3) {
            observedValue = null

            p.emit()

            assertNotNull(observedValue)
            assertEquals(p.observable.value, observedValue)
        }
    }

    /**
     * When [Cell.observe] is called with callNow = true, it should call the observer immediately.
     * Otherwise it should only call the observer when it changes.
     */
    @Test
    fun respects_call_now_argument() = test {
        val p = createProvider()
        var changes = 0

        // Test callNow = false
        p.observable.observe(callNow = false) {
            changes++
        }.use {
            p.emit()

            assertEquals(1, changes)
        }

        // Test callNow = true
        changes = 0

        p.observable.observe(callNow = true) {
            changes++
        }.use {
            p.emit()

            assertEquals(2, changes)
        }
    }

    /**
     * [Cell.value] should correctly reflect changes even when the [Cell] has no observers.
     * Typically this means that the cell's value is not updated in real time, only when it is
     * queried.
     */
    @Test
    fun reflects_changes_without_observers() = test {
        val p = createProvider()

        var old: Any?

        repeat(5) {
            // Value should change after emit.
            old = p.observable.value

            p.emit()

            val new = p.observable.value

            assertNotEquals(old, new)

            // Value should not change when emit hasn't been called since the last access.
            assertEquals(new, p.observable.value)
        }
    }

    interface Provider : ObservableTests.Provider {
        override val observable: Cell<Any>
    }
}
