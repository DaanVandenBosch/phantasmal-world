package world.phantasmal.testUtils

import world.phantasmal.core.disposable.DisposableScope
import world.phantasmal.core.disposable.Scope
import world.phantasmal.core.disposable.TrackedDisposable
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

abstract class TestSuite {
    private var initialDisposableCount: Int = 0
    private var _scope: DisposableScope? = null

    protected val scope: Scope get() = _scope!!

    @BeforeTest
    fun before() {
        initialDisposableCount = TrackedDisposable.disposableCount
        _scope = DisposableScope()
    }

    @AfterTest
    fun after() {
        _scope!!.dispose()

        val leakCount = TrackedDisposable.disposableCount - initialDisposableCount
        assertEquals(0, leakCount, "TrackedDisposables were leaked")
    }
}
