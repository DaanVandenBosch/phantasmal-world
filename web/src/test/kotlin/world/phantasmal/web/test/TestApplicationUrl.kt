package world.phantasmal.web.test

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.web.core.stores.ApplicationUrl

class PathAndParams(private val pathAndParams: String) {
    val path: String
    val params: Map<String, String?>

    init {
        val pathAndParamSplit = pathAndParams.split("?")
        path = pathAndParamSplit[0]
        val paramsStr = pathAndParamSplit.getOrNull(1)

        if (paramsStr == null) {
            params = emptyMap()
        } else {
            val params = mutableMapOf<String, String?>()

            for (paramNameAndValue in paramsStr.split("&")) {
                val paramNameAndValueSplit = paramNameAndValue.split("=", limit = 2)
                val paramName = paramNameAndValueSplit[0]
                val value = paramNameAndValueSplit.getOrNull(1)

                params[paramName] = value
            }

            this.params = params
        }
    }

    override fun toString(): String = pathAndParams
}

class TestApplicationUrl(initialPathAndParams: String) : ApplicationUrl {
    private val stack = mutableListOf(PathAndParams(initialPathAndParams))
    private var stackIdx = 0 // Points to the current URL entry in the stack.
    private val popCallbacks = mutableListOf<(String) -> Unit>()

    // Super type properties.

    override val pathAndParams: String get() = stack[stackIdx].toString()

    // Extra properties used during tests.

    val pathAndParamsDeconstructed: PathAndParams get() = stack[stackIdx]
    val historyEntries: Int get() = stackIdx
    val canGoBack: Boolean get() = stackIdx > 0
    val canGoForward: Boolean get() = stackIdx < stack.lastIndex

    // Super type methods.

    override fun pushPathAndParams(pathAndParams: String) {
        while (stack.lastIndex < stackIdx) stack.removeLast()
        stackIdx++
        stack.add(PathAndParams(pathAndParams))
    }

    override fun replacePathAndParams(pathAndParams: String) {
        stack[stackIdx] = PathAndParams(pathAndParams)
    }

    override fun onPopPathAndParams(callback: (String) -> Unit): Disposable {
        popCallbacks.add(callback)
        return disposable { popCallbacks.remove(callback) }
    }

    // Extra methods used during tests.

    /** Simulates browser back. */
    fun back() {
        check(canGoBack)
        stackIdx--
        callPopCallbacks()
    }

    /** Simulates browser forward. */
    fun forward() {
        check(canGoForward)
        stackIdx++
        callPopCallbacks()
    }

    /** Simulates the user editing the browser URL. */
    fun navigate(pathAndParams: String) {
        pushPathAndParams(pathAndParams)
        callPopCallbacks()
    }

    private fun callPopCallbacks() {
        val newPathAndParams = pathAndParams

        for (callback in popCallbacks) {
            callback(newPathAndParams)
        }
    }
}
