package world.phantasmal.web.shared

import world.phantasmal.web.shared.externals.self

/**
 * Helper for limiting the amount of times a function is called within a given window.
 *
 * @param wait The number of milliseconds to throttle invocations to.
 * @param leading Invoke on the leading edge of the timeout window.
 * @param trailing Invoke on the trailing edge of the timeout window.
 */
class Throttle(
    private val wait: Int,
    private val leading: Boolean = true,
    private val trailing: Boolean = true,
) {
    private var timeout: Int? = null
    private var invokeOnTimeout = false
    private var function: () -> Unit = {}

    operator fun invoke(f: () -> Unit) {
        function = f

        if (timeout == null) {
            if (leading) {
                function()
            } else if (trailing) {
                invokeOnTimeout = true
            }

            timeout = self.setTimeout({
                if (invokeOnTimeout) {
                    function()
                }

                timeout = null
                invokeOnTimeout = false
            }, wait)
        } else {
            invokeOnTimeout = trailing
        }
    }
}
