package world.phantasmal.web

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import org.w3c.dom.PopStateEvent
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.DisposableScope
import world.phantasmal.core.disposable.Scope
import world.phantasmal.core.disposable.disposable
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.web.application.Application
import world.phantasmal.web.core.HttpAssetLoader
import world.phantasmal.web.core.UiDispatcher
import world.phantasmal.web.core.stores.ApplicationUrl
import world.phantasmal.web.externals.Engine
import world.phantasmal.webui.dom.disposableListener
import world.phantasmal.webui.dom.root

fun main() {
    if (document.body != null) {
        init()
    } else {
        window.addEventListener("DOMContentLoaded", { init() })
    }
}

private fun init(): Disposable {
    val scope = DisposableScope()

    val crScope = CoroutineScope(UiDispatcher)
    scope.disposable { crScope.cancel() }

    val rootElement = document.body!!.root()

    val httpClient = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            })
        }
    }
    scope.disposable { httpClient.cancel() }

    val pathname = window.location.pathname
    val basePath = window.location.origin +
            (if (pathname.lastOrNull() == '/') pathname.dropLast(1) else pathname)

    Application(
        scope,
        crScope,
        rootElement,
        HttpAssetLoader(httpClient, basePath),
        HistoryApplicationUrl(scope),
        createEngine = { Engine(it) }
    )

    return scope
}

class HistoryApplicationUrl(scope: Scope) : ApplicationUrl {
    private val path: String get() = window.location.pathname

    override val url = mutableVal(window.location.hash.substring(1))

    init {
        disposableListener<PopStateEvent>(scope, window, "popstate", {
            url.value = window.location.hash.substring(1)
        })
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
