package world.phantasmal.core.disposable

import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackedDisposableTests {
    @Test
    fun is_correctly_tracked() {
        assertFails {
            checkNoDisposableLeaks {
                object : TrackedDisposable() {}
            }
        }

        checkNoDisposableLeaks {
            val disposable = object : TrackedDisposable() {}
            disposable.dispose()
        }
    }

    @Test
    fun double_dispose_throws() {
        val disposable = object : TrackedDisposable() {}

        disposable.dispose()

        assertFails {
            disposable.dispose()
        }
    }

    @Test
    fun disposed_property_is_set_correctly() {
        val disposable = object : TrackedDisposable() {}

        assertFalse(disposable.disposed)

        disposable.dispose()

        assertTrue(disposable.disposed)
    }
}
