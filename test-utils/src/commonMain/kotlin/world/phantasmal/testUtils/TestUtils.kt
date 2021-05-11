package world.phantasmal.testUtils

import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Ensure you return the value of this function in your test function. On Kotlin/JS this function
 * actually returns a Promise. If this promise is not returned from the test function, the testing
 * framework won't wait for its completion. This is a workaround for issue
 * [https://youtrack.jetbrains.com/issue/KT-22228].
 */
internal expect fun testAsync(block: suspend () -> Unit)

internal expect fun canExecuteSlowTests(): Boolean

fun <T> assertDeepEquals(
    expected: List<T>,
    actual: List<T>,
    assertDeepEquals: (T, T, String?) -> Unit,
    message: String? = null,
) {
    assertEquals(
        expected.size,
        actual.size,
        "Unexpected list size" + (if (message == null) "" else ". $message"),
    )

    for (i in expected.indices) {
        assertDeepEquals(expected[i], actual[i], message)
    }
}

fun <K, V> assertDeepEquals(
    expected: Map<K, V>,
    actual: Map<K, V>,
    assertDeepEquals: (V, V, String?) -> Unit,
    message: String? = null
) {
    assertEquals(
        expected.size,
        actual.size,
        "Unexpected map size" + (if (message == null) "" else ". $message"),
    )

    for ((key, value) in expected) {
        assertTrue(key in actual, message)
        assertDeepEquals(value, actual[key]!!, message)
    }
}
