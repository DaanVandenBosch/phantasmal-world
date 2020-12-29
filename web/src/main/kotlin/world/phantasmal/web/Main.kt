package world.phantasmal.web

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.cancel
import kotlinx.datetime.Clock
import mu.KotlinLoggingConfiguration
import mu.KotlinLoggingLevel
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.PopStateEvent
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.web.application.Application
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.logging.LogAppender
import world.phantasmal.web.core.logging.LogFormatter
import world.phantasmal.web.core.rendering.DisposableThreeRenderer
import world.phantasmal.web.core.stores.ApplicationUrl
import world.phantasmal.web.externals.three.WebGLRenderer
import world.phantasmal.webui.dom.disposableListener
import world.phantasmal.webui.dom.root
import world.phantasmal.webui.obj

fun main() {
    if (document.body != null) {
        init()
    } else {
        window.addEventListener("DOMContentLoaded", { init() })
    }
}

private fun init(): Disposable {
    KotlinLoggingConfiguration.FORMATTER = LogFormatter()
    KotlinLoggingConfiguration.APPENDER = LogAppender()

    if (window.location.hostname == "localhost") {
        KotlinLoggingConfiguration.LOG_LEVEL = KotlinLoggingLevel.TRACE
    }

    val disposer = Disposer()

    val rootElement = document.body!!.root()

    val httpClient = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            })
        }
    }
    disposer.add(disposable { httpClient.cancel() })

    disposer.add(
        Application(
            rootElement,
            AssetLoader(httpClient),
            disposer.add(HistoryApplicationUrl()),
            ::createThreeRenderer,
            Clock.System,
        )
    )

    return disposer
}

private fun createThreeRenderer(canvas: HTMLCanvasElement): DisposableThreeRenderer =
    object : TrackedDisposable(), DisposableThreeRenderer {
        override val renderer = WebGLRenderer(obj {
            this.canvas = canvas
            antialias = true
            alpha = true
        })

        init {
            renderer.setPixelRatio(window.devicePixelRatio)
        }

        override fun dispose() {
            renderer.dispose()
            super.dispose()
        }
    }

private class HistoryApplicationUrl : TrackedDisposable(), ApplicationUrl {
    private val path: String get() = window.location.pathname

    override val url = mutableVal(window.location.hash.substring(1))

    private val popStateListener = window.disposableListener<PopStateEvent>("popstate", {
        url.value = window.location.hash.substring(1)
    })

    override fun dispose() {
        popStateListener.dispose()
        super.dispose()
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
