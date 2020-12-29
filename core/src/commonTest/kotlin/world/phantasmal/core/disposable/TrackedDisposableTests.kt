package world.phantasmal.core.disposable

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackedDisposableTests {
    @Test
    fun count_goes_up_when_created_and_down_when_disposed() {
        val initialCount = TrackedDisposable.disposableCount

        val disposable = object : TrackedDisposable() {}

        assertEquals(initialCount + 1, TrackedDisposable.disposableCount)

        disposable.dispose()

        assertEquals(initialCount, TrackedDisposable.disposableCount)
    }

    @Test
    fun double_dispose_does_not_increase_count() {
        val initialCount = TrackedDisposable.disposableCount

        val disposable = object : TrackedDisposable() {}

        for (i in 1..5) {
            disposable.dispose()
        }

        assertEquals(initialCount, TrackedDisposable.disposableCount)
    }

    @Test
    fun disposed_property_is_set_correctly() {
        val disposable = object : TrackedDisposable() {}

        assertFalse(disposable.disposed)

        disposable.dispose()

        assertTrue(disposable.disposed)
    }
}
