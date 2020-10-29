package world.phantasmal.observable

import world.phantasmal.testUtils.TestSuite
import kotlin.test.Test
import kotlin.test.assertEquals

typealias ObservableAndEmit = Pair<Observable<*>, () -> Unit>

/**
 * Test suite for all [Observable] implementations. There is a subclass of this suite for every
 * [Observable] implementation.
 */
abstract class ObservableTests : TestSuite() {
    protected abstract fun create(): ObservableAndEmit

    @Test
    fun observable_calls_observers_when_events_are_emitted() = test {
        val (observable, emit) = create()
        var changes = 0

        disposer.add(
            observable.observe {
                changes++
            }
        )

        emit()

        assertEquals(1, changes)

        emit()
        emit()
        emit()

        assertEquals(4, changes)
    }

    @Test
    fun observable_does_not_call_observers_after_they_are_disposed() = test {
        val (observable, emit) = create()
        var changes = 0

        val observer = observable.observe {
            changes++
        }

        emit()

        assertEquals(1, changes)

        observer.dispose()

        emit()
        emit()
        emit()

        assertEquals(1, changes)
    }
}
