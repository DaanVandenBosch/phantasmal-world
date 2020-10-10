package world.phantasmal.web

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.PopStateEvent
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.DisposableContainer
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.web.application.Application
import world.phantasmal.web.core.stores.ApplicationUrl
import world.phantasmal.webui.dom.disposableListener

fun main() {
    if (document.body != null) {
        init()
    } else {
        window.addEventListener("DOMContentLoaded", { init() })
    }
}

private fun init(): Disposable {
    val disposer = Disposer()

    val rootNode = document.body!!

    disposer.add(Application(rootNode, HistoryApplicationUrl()))

    return disposer
}

class HistoryApplicationUrl : DisposableContainer(), ApplicationUrl {
    private val path: String get() = window.location.pathname

    override val url = mutableVal(window.location.hash.substring(1))

    init {
        addDisposable(disposableListener<PopStateEvent>(window, "popstate", {
            url.value = window.location.hash.substring(1)
        }))
    }

    override fun pushUrl(url: String) {
        window.history.pushState(null, TITLE, "$path#$url")
        // Do after pushState to avoid triggering observers that call pushUrl or replaceUrl before
        // the current change has happened.
        this.url.value = url
    }

    override fun replaceUrl(url: String) {
        window.history.replaceState(null, TITLE, "$path#$url")
        // Do after replaceState to avoid triggering observers that call pushUrl or replaceUrl
        // before the current change has happened.
        this.url.value = url
    }

    companion object {
        private const val TITLE = "Phantasmal World"
    }
}
