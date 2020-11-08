package world.phantasmal.testUtils

/**
 * Ensure you return the value of this function in your test function. On Kotlin/JS this function
 * actually returns a Promise. If this promise is not returned from the test function, the testing
 * framework won't wait for its completion. This is a workaround for issue
 * [https://youtrack.jetbrains.com/issue/KT-22228].
 */
expect fun asyncTest(block: suspend () -> Unit)
