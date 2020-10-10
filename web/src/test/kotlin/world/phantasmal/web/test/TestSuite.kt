package world.phantasmal.web.test

import world.phantasmal.core.disposable.TrackedDisposable
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

abstract class TestSuite {
    private var initialDisposableCount: Int = 0

    @BeforeTest
    fun before() {
        initialDisposableCount = TrackedDisposable.disposableCount
    }

    @AfterTest
    fun after() {
        assertEquals(
            initialDisposableCount,
            TrackedDisposable.disposableCount,
            "TrackedDisposables were leaked"
        )
    }
}
