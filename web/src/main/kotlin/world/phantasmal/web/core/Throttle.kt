package world.phantasmal.web.core

import kotlinx.browser.window

/**
 * Helper for limiting the amount of times a function is called within a given time frame.
 *
 * @param before the amount of time in ms before the function is actually called. E.g., if [before]
 * is 10 and [invoke] is called once, the given function won't be called until 10ms have passed. If
 * invoke is called again before the 10ms have passed, it will still only be called once after 10ms
 * have passed since the first call to invoke.
 */
class Throttle(private val before: Int) {
    private var timeout: Int = -1

    operator fun invoke(f: () -> Unit) {
        if (timeout == -1) {
            timeout = window.setTimeout({
                f()
                timeout = -1
            }, before)
        }
    }
}
