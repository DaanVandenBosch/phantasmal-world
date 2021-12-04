package world.phantasmal.observable.cell

import world.phantasmal.core.disposable.use
import world.phantasmal.observable.ChangeEvent
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

        // We literally just test that accessing the value property doesn't throw or return null.
        assertNotNull(p.observable.value)
    }

    @Test
    fun value_is_accessible_with_observers() = test {
        val p = createProvider()

        disposer.add(p.observable.observeChange {})

        // We literally just test that accessing the value property doesn't throw or return null.
        assertNotNull(p.observable.value)
    }

    @Test
    fun emits_no_change_event_until_changed() = test {
        val p = createProvider()

        var observedEvent: ChangeEvent<Any>? = null

        disposer.add(p.observable.observeChange { changeEvent ->
            observedEvent = changeEvent
        })

        assertNull(observedEvent)

        p.emit()

        assertNotNull(observedEvent)
    }

    @Test
    fun emits_correct_value_in_change_events() = test {
        val p = createProvider()

        var prevValue: Any?
        var observedValue: Any? = null

        disposer.add(p.observable.observeChange { changeEvent ->
            assertNull(observedValue)
            observedValue = changeEvent.value
        })

        repeat(3) {
            prevValue = observedValue
            observedValue = null

            p.emit()

            // We should have observed a value, it should be different from the previous value, and
            // it should be equal to the cell's current value.
            assertNotNull(observedValue)
            assertNotEquals(prevValue, observedValue)
            assertEquals(p.observable.value, observedValue)
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

    //
    // CellUtils Tests
    //

    @Test
    fun propagates_changes_to_observeNow_observers() = test {
        val p = createProvider()
        var changes = 0

        p.observable.observeNow {
            changes++
        }.use {
            p.emit()

            assertEquals(2, changes)
        }
    }

    @Test
    fun propagates_changes_to_mapped_cell() = test {
        val p = createProvider()
        val mapped = p.observable.map { it.hashCode() }
        val initialValue = mapped.value

        var observedValue: Int? = null

        disposer.add(mapped.observeChange { changeEvent ->
            assertNull(observedValue)
            observedValue = changeEvent.value
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

        disposer.add(mapped.observeChange {
            assertNull(observedValue)
            observedValue = it.value
        })

        p.emit()

        assertNotEquals(initialValue, mapped.value)
        assertEquals(mapped.value, observedValue)
    }

    interface Provider : ObservableTests.Provider {
        override val observable: Cell<Any>
    }
}
