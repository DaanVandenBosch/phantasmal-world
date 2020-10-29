package world.phantasmal.observable.value

import world.phantasmal.core.disposable.use
import world.phantasmal.observable.ObservableTests
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
    fun val_respects_call_now_argument() = test {
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
