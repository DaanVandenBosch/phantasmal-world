package world.phantasmal.observable

import world.phantasmal.observable.test.withScope
import world.phantasmal.testUtils.TestSuite
import kotlin.test.Test
import kotlin.test.assertEquals

typealias ObservableAndEmit = Pair<Observable<*>, () -> Unit>

/**
 * Test suite for all [Observable] implementations. There is a subclass of this suite for every
 * [Observable] implementation.
 */
abstract class ObservableTests : TestSuite() {
    abstract fun create(): ObservableAndEmit

    @Test
    fun observable_calls_observers_when_events_are_emitted() {
        val (observable, emit) = create()
        val changes = mutableListOf<ChangeEvent<*>>()

        withScope { scope ->
            observable.observe(scope) { c ->
                changes.add(c)
            }

            emit()

            assertEquals(1, changes.size)

            emit()
            emit()
            emit()

            assertEquals(4, changes.size)
        }
    }

    @Test
    fun observable_does_not_call_observers_after_they_are_disposed() {
        val (observable, emit) = create()
        val changes = mutableListOf<ChangeEvent<*>>()

        withScope { scope ->
            observable.observe(scope) { c ->
                changes.add(c)
            }

            emit()

            assertEquals(1, changes.size)

            emit()
            emit()
            emit()

            assertEquals(4, changes.size)
        }
    }
}
