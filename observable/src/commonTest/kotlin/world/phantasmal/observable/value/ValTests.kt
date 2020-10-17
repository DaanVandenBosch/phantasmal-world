package world.phantasmal.observable.value

import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.ObservableTests
import world.phantasmal.observable.test.withScope
import kotlin.test.Test
import kotlin.test.assertEquals

typealias ValAndEmit<T> = Pair<Val<T>, () -> Unit>

/**
 * Test suite for all [Val] implementations. There is a subclass of this suite for every [Val]
 * implementation.
 */
abstract class ValTests : ObservableTests() {
    abstract override fun create(): ValAndEmit<*>

    /**
     * When [Val.observe] is called with callNow = true, it should call the observer immediately.
     * Otherwise it should only call the observer when it changes.
     */
    @Test
    fun val_respects_call_now_argument() {
        val (value, emit) = create()
        val changes = mutableListOf<ChangeEvent<*>>()

        withScope { scope ->
            // Test callNow = false
            value.observe(scope, callNow = false) { c ->
                changes.add(c)
            }

            emit()

            assertEquals(1, changes.size)
        }

        withScope { scope ->
            // Test callNow = true
            changes.clear()

            value.observe(scope, callNow = true) { c ->
                changes.add(c)
            }

            emit()

            assertEquals(2, changes.size)
        }
    }
}
