package world.phantasmal.core.disposable

import kotlinx.coroutines.Job
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackedDisposableTests {
    @Test
    fun count_should_go_up_when_created_and_down_when_disposed() {
        val initialCount = TrackedDisposable.disposableCount

        val disposable = object : TrackedDisposable(DummyScope()) {
            override fun internalDispose() {}
        }

        assertEquals(initialCount + 1, TrackedDisposable.disposableCount)

        disposable.dispose()

        assertEquals(initialCount, TrackedDisposable.disposableCount)
    }

    @Test
    fun double_dispose_should_not_increase_count() {
        val initialCount = TrackedDisposable.disposableCount

        val disposable = object : TrackedDisposable(DummyScope()) {
            override fun internalDispose() {}
        }

        for (i in 1..5) {
            disposable.dispose()
        }

        assertEquals(initialCount, TrackedDisposable.disposableCount)
    }

    @Test
    fun disposed_property_should_be_set_correctly() {
        val disposable = object : TrackedDisposable(DummyScope()) {
            override fun internalDispose() {}
        }

        assertFalse(disposable.disposed)

        disposable.dispose()

        assertTrue(disposable.disposed)
    }

    private class DummyScope : Scope {
        override val coroutineContext = Job()

        override fun add(disposable: Disposable) {
            // Do nothing.
        }

        override fun scope(): Scope = throw NotImplementedError()
    }
}
