package world.phantasmal.observable.value

import world.phantasmal.core.disposable.use
import world.phantasmal.observable.ObservableTests
import kotlin.test.*

/**
 * Test suite for all [Val] implementations. There is a subclass of this suite for every [Val]
 * implementation.
 */
interface ValTests : ObservableTests {
    override fun createProvider(): Provider

    @Test
    fun propagates_changes_to_mapped_val() = test {
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
    fun propagates_changes_to_flat_mapped_val() = test {
        val p = createProvider()

        val mapped = p.observable.flatMap { StaticVal(it.hashCode()) }
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
     * When [Val.observe] is called with callNow = true, it should call the observer immediately.
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

    interface Provider : ObservableTests.Provider {
        override val observable: Val<Any>
    }
}
