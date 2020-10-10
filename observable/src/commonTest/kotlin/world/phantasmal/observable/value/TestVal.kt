package world.phantasmal.observable.value

// Test suite for all Val implementations.
// These functions are called from type-specific unit tests.

import world.phantasmal.core.disposable.use
import world.phantasmal.observable.ChangeEvent
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

typealias ValAndEmit<T> = Pair<Val<T>, () -> Unit>

fun valTests(
    create: () -> ValAndEmit<*>,
    createBoolean: ((Boolean) -> ValAndEmit<Boolean>)?,
) {
    valShouldRespectCallNowArgument(create)

    if (createBoolean != null) {
        testValBooleanExtensions(createBoolean)
    }
}

/**
 * When Val::observe is called with callNow = true, it should call the observer immediately.
 * Otherwise it should only call the observer when it changes.
 */
private fun valShouldRespectCallNowArgument(create: () -> ValAndEmit<*>) {
    val (value, emit) = create()
    val changes = mutableListOf<ChangeEvent<*>>()

    // Test callNow = true
    value.observe(callNow = false) { c ->
        changes.add(c)
    }.use {
        emit()

        assertEquals(1, changes.size)
    }

    // Test callNow = false
    changes.clear()

    value.observe(callNow = true) { c ->
        changes.add(c)
    }.use {
        emit()

        assertEquals(2, changes.size)
    }
}

private fun testValBooleanExtensions(create: (Boolean) -> ValAndEmit<Boolean>) {
    listOf(true, false).forEach { bool ->
        val (value) = create(bool)

        // Test the test setup first.
        assertEquals(bool, value.value)

        // Test `and`.
        assertEquals(bool, (value and trueVal()).value)
        assertFalse((value and falseVal()).value)

        // Test `or`.
        assertTrue((value or trueVal()).value)
        assertEquals(bool, (value or falseVal()).value)

        // Test `xor`.
        assertEquals(!bool, (value xor trueVal()).value)
        assertEquals(bool, (value xor falseVal()).value)

        // Test `!` (unary not).
        assertEquals(!bool, (!value).value)
    }
}
