package world.phantasmal.observable

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test suite for all [Observable] implementations. There is a subclass of this suite for every
 * [Observable] implementation.
 */
interface ObservableTests : DependencyTests {
    override fun createProvider(): Provider

    @Test
    fun calls_observers_when_events_are_emitted() = test {
        val p = createProvider()
        var changes = 0

        disposer.add(
            p.observable.observe {
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

        val observer = p.observable.observe {
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

    interface Provider : DependencyTests.Provider {
        val observable: Observable<*>

        override val dependency: Dependency get() = observable
    }
}
