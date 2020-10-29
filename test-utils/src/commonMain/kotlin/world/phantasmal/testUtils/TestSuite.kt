package world.phantasmal.testUtils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.core.disposable.TrackedDisposable
import kotlin.test.assertEquals

abstract class TestSuite {
    fun test(block: TestContext.() -> Unit) {
        val initialDisposableCount = TrackedDisposable.disposableCount
        val disposer = Disposer()

        block(TestContext(disposer))

        disposer.dispose()
        val leakCount = TrackedDisposable.disposableCount - initialDisposableCount
        assertEquals(0, leakCount, "TrackedDisposables were leaked")
    }

    class TestContext(val disposer: Disposer) {
        val scope: CoroutineScope = object : CoroutineScope {
            override val coroutineContext = Job()
        }
    }
}
