package world.phantasmal.web.core.observable

import world.phantasmal.web.test.WebTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals

class EmitterTests : WebTestSuite {
    @Test
    fun calls_observers_when_events_are_emitted() = test {
        val emitter = Emitter<Int>()
        var changes = 0

        disposer.add(
            emitter.observe {
                assertEquals(changes, it)
                changes++
            }
        )

        emitter.emit(0)

        assertEquals(1, changes)

        emitter.emit(1)
        emitter.emit(2)
        emitter.emit(3)

        assertEquals(4, changes)
    }

    @Test
    fun does_not_call_observers_after_they_are_disposed() = test {
        val emitter = Emitter<Int>()
        var changes = 0

        val observer = emitter.observe {
            assertEquals(changes, it)
            changes++
        }

        emitter.emit(0)

        assertEquals(1, changes)

        observer.dispose()

        emitter.emit(1)
        emitter.emit(2)
        emitter.emit(3)

        assertEquals(1, changes)
    }
}
