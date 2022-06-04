package world.phantasmal.cell

import world.phantasmal.cell.test.CellTestSuite
import world.phantasmal.core.disposable.use
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test suite for all [Cell] implementations. There is a subclass of this suite for every [Cell]
 * implementation.
 */
interface CellTests : CellTestSuite {
    fun createProvider(): Provider

    /**
     * Tests low level [Dependency] implementation.
     */
    @Test
    fun correctly_emits_invalidation_notifications_to_its_dependents() = test {
        val p = createProvider()
        var dependencyInvalidatedCalled: Boolean

        p.cell.addDependent(object : Dependent {
            override fun dependencyInvalidated(dependency: Dependency<*>) {
                assertEquals(p.cell, dependency)
                dependencyInvalidatedCalled = true
            }
        })

        repeat(5) { index ->
            dependencyInvalidatedCalled = false

            p.emit()

            assertTrue(dependencyInvalidatedCalled, "repetition $index")
        }
    }

    @Test
    fun value_is_accessible_without_observers() = test {
        val p = createProvider()

        // We literally just test that accessing the value property doesn't throw or return null.
        assertNotNull(p.cell.value)
    }

    @Test
    fun value_is_accessible_with_observers() = test {
        val p = createProvider()

        disposer.add(p.cell.observeChange {})

        // We literally just test that accessing the value property doesn't throw or return null.
        assertNotNull(p.cell.value)
    }

    @Test
    fun calls_observers_when_events_are_emitted() = test {
        val p = createProvider()
        var changes = 0

        disposer.add(
            p.cell.observeChange {
                changes++
            }
        )

        p.emit()

        assertEquals(1, changes)

        p.emit()
        p.emit()
        p.emit()

        assertEquals(4, changes)
    }

    @Test
    fun does_not_call_observers_after_they_are_disposed() = test {
        val p = createProvider()
        var changes = 0

        val observer = p.cell.observeChange {
            changes++
        }

        p.emit()

        assertEquals(1, changes)

        observer.dispose()

        p.emit()
        p.emit()
        p.emit()

        assertEquals(1, changes)
    }

    @Test
    fun emits_no_change_event_until_changed() = test {
        val p = createProvider()

        var observedEvent: ChangeEvent<Any>? = null

        disposer.add(p.cell.observeChange { changeEvent ->
            observedEvent = changeEvent
        })

        assertNull(observedEvent)

        p.emit()

        assertNotNull(observedEvent)
    }

    @Test
    fun emits_correct_value_in_change_events() = test {
        val p = createProvider()

        var prevValue: Snapshot?
        var observedValue: Snapshot? = null

        disposer.add(p.cell.observeChange { changeEvent ->
            assertNull(observedValue)
            observedValue = changeEvent.value.snapshot()
        })

        repeat(3) {
            prevValue = observedValue
            observedValue = null

            p.emit()

            // We should have observed a value, it should be different from the previous value, and
            // it should be equal to the cell's current value.
            assertNotNull(observedValue)
            assertNotEquals(prevValue, observedValue)
            assertEquals(p.cell.value.snapshot(), observedValue)
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

        var old: Snapshot?

        repeat(5) {
            // Value should change after emit.
            old = p.cell.value.snapshot()

            p.emit()

            val new = p.cell.value.snapshot()

            assertNotEquals(old, new)

            // Value should not change when emit hasn't been called since the last access.
            assertEquals(new, p.cell.value.snapshot())
        }
    }

    //
    // CellUtils Tests
    //

    @Test
    fun propagates_changes_to_observeNow_observers() = test {
        val p = createProvider()
        var changes = 0

        p.cell.observeNow {
            changes++
        }.use {
            p.emit()

            assertEquals(2, changes)
        }
    }

    @Test
    fun propagates_changes_to_mapped_cell() = test {
        val p = createProvider()
        val mapped = p.cell.map { it.snapshot() }
        val initialValue = mapped.value

        var observedValue: Snapshot? = null

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

        val mapped = p.cell.flatMap { ImmutableCell(it.snapshot()) }
        val initialValue = mapped.value

        var observedValue: Snapshot? = null

        disposer.add(mapped.observeChange {
            assertNull(observedValue)
            observedValue = it.value
        })

        p.emit()

        assertNotEquals(initialValue, mapped.value)
        assertEquals(mapped.value, observedValue)
    }

    interface Provider {
        val cell: Cell<Any>

        /**
         * Makes [cell] emit a change.
         */
        fun emit()
    }
}

/** See [snapshot]. */
private typealias Snapshot = String

/**
 * We use toString to create "snapshots" of values throughout the tests. Most of the time cells will
 * actually have a new value after emitting a change event, but this is not always the case with
 * more complex cells or cells that point to complex values. So instead of keeping references to
 * values and comparing them with == (or using e.g. assertEquals), we compare snapshots.
 *
 * This of course assumes that all values have sensible toString implementations.
 */
private fun Any?.snapshot(): Snapshot = toString()
