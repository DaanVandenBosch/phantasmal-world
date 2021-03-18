package world.phantasmal.observable.value

import world.phantasmal.core.disposable.use
import world.phantasmal.observable.ObservableAndEmit
import world.phantasmal.observable.ObservableTests
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

interface ValAndEmit : ObservableAndEmit {
    override val observable: Val<Any>

    override fun component1(): Val<Any> = observable
}

/**
 * Test suite for all [Val] implementations. There is a subclass of this suite for every [Val]
 * implementation.
 */
abstract class ValTests : ObservableTests() {
    abstract override fun create(): ValAndEmit

    @Test
    fun propagates_changes_to_mapped_val() = test {
        val (value, emit) = create()
        val mapped = value.map { it.hashCode() }
        val initialValue = mapped.value

        var observedValue: Int? = null

        disposer.add(mapped.observe {
            assertNull(observedValue)
            observedValue = it.value
        })

        emit()

        assertNotEquals(initialValue, mapped.value)
        assertEquals(mapped.value, observedValue)
    }

    /**
     * When [Val.observe] is called with callNow = true, it should call the observer immediately.
     * Otherwise it should only call the observer when it changes.
     */
    @Test
    fun respects_call_now_argument() = test {
        val (value, emit) = create()
        var changes = 0

        // Test callNow = false
        value.observe(callNow = false) {
            changes++
        }.use {
            emit()

            assertEquals(1, changes)
        }

        // Test callNow = true
        changes = 0

        value.observe(callNow = true) {
            changes++
        }.use {
            emit()

            assertEquals(2, changes)
        }
    }
}
