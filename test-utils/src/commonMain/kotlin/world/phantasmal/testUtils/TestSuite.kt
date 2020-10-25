package world.phantasmal.testUtils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.core.disposable.TrackedDisposable
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

abstract class TestSuite {
    private var initialDisposableCount: Int = 0
    private var _disposer: Disposer? = null

    protected val disposer: Disposer get() = _disposer!!

    protected val scope: CoroutineScope = object : CoroutineScope {
        override val coroutineContext = Job()
    }

    @BeforeTest
    fun before() {
        initialDisposableCount = TrackedDisposable.disposableCount
        _disposer = Disposer()
    }

    @AfterTest
    fun after() {
        _disposer!!.dispose()

        val leakCount = TrackedDisposable.disposableCount - initialDisposableCount
        assertEquals(0, leakCount, "TrackedDisposables were leaked")
    }
}
