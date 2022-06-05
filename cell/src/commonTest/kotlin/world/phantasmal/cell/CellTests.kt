package world.phantasmal.cell

import world.phantasmal.cell.test.CellTestSuite
import world.phantasmal.cell.test.Snapshot
import world.phantasmal.cell.test.snapshot
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

    //
    // Mutation tests.
    //

    @Test
    fun changes_during_a_mutation_are_deferred() = test {
        val p = createProvider()
        var changes = 0

        disposer.add(
            p.cell.observeChange {
                changes++
            }
        )

        mutate {
            repeat(5) {
                p.emit()

                // Change should be deferred until this lambda returns.
                assertEquals(0, changes)
            }
        }

        // All changes to the same cell should be collapsed to a single change.
        assertEquals(1, changes)
    }

    @Test
    fun value_can_be_accessed_during_a_mutation() = test {
        val p = createProvider()

        // Change will be observed exactly once.
        var observedValue: Snapshot? = null

        disposer.add(
            p.cell.observeChange {
                assertNull(observedValue)
                observedValue = it.value.snapshot()
            }
        )

        val v1 = p.cell.value.snapshot()
        var v3: Snapshot? = null

        mutate {
            val v2 = p.cell.value.snapshot()

            assertEquals(v1, v2)

            p.emit()
            v3 = p.cell.value.snapshot()

            assertNotEquals(v2, v3)

            p.emit()
        }

        val v4 = p.cell.value.snapshot()

        assertNotNull(v3)
        assertNotEquals(v3, v4)
        assertEquals(v4, observedValue)
    }

    @Test
    fun mutations_can_be_nested() = test {
        // 3 Cells.
        val ps = Array(3) { createProvider() }
        val observedChanges = IntArray(3)

        // Observe each cell.
        repeat(3) { idx ->
            disposer.add(
                ps[idx].cell.observeChange {
                    assertEquals(0, observedChanges[idx])
                    observedChanges[idx]++
                }
            )
        }

        mutate {
            ps[0].emit()

            repeat(3) {
                mutate {
                    ps[1].emit()

                    mutate {
                        ps[2].emit()
                    }

                    assertTrue(observedChanges.all { it == 0 })
                }

                assertTrue(observedChanges.all { it == 0 })
            }
        }

        // At this point all 3 observers should be called exactly once.
        assertTrue(observedChanges.all { it == 1 })
    }

    interface Provider {
        val cell: Cell<Any>

        /**
         * Makes [cell] emit a change.
         */
        fun emit()
    }
}
