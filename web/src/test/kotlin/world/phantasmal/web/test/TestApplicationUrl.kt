package world.phantasmal.web.test

import world.phantasmal.observable.cell.MutableCell
import world.phantasmal.observable.cell.mutableCell
import world.phantasmal.web.core.stores.ApplicationUrl

class TestApplicationUrl(initialUrl: String) : ApplicationUrl {
    private val stack = mutableListOf(initialUrl)
    private var stackIdx = 0 // Points to the current URL entry in the stack.

    override val url: MutableCell<String> = mutableCell(initialUrl)

    val historyEntries: Int get() = stackIdx
    val canGoBack: Boolean get() = stackIdx > 0
    val canGoForward: Boolean get() = stackIdx < stack.lastIndex

    override fun pushUrl(url: String) {
        while (stack.lastIndex < stackIdx) stack.removeLast()
        stackIdx++
        stack.add(url)
        this.url.value = url
    }

    override fun replaceUrl(url: String) {
        stack[stackIdx] = url
        this.url.value = url
    }

    fun back() {
        check(canGoBack)
        this.url.value = stack[--stackIdx]
    }

    fun forward() {
        check(canGoForward)
        this.url.value = stack[++stackIdx]
    }
}
