package world.phantasmal.web

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.datetime.Clock
import mu.KotlinLoggingConfiguration
import mu.KotlinLoggingLevel
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.PopStateEvent
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.web.application.Application
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.persistence.LocalStorageKeyValueStore
import world.phantasmal.web.core.rendering.DisposableThreeRenderer
import world.phantasmal.web.core.stores.ApplicationUrl
import world.phantasmal.web.externals.three.WebGLRenderer
import world.phantasmal.web.shared.logging.LogAppender
import world.phantasmal.web.shared.logging.LogFormatter
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

    disposer.add(
        Application(
            rootElement,
            LocalStorageKeyValueStore(),
            AssetLoader(),
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
            renderer.debug.checkShaderErrors = false
            renderer.setPixelRatio(window.devicePixelRatio)
        }

        override fun dispose() {
            renderer.dispose()
            super.dispose()
        }
    }

private class HistoryApplicationUrl : TrackedDisposable(), ApplicationUrl {
    private val path: String get() = window.location.pathname
    private val popCallbacks = mutableListOf<(String) -> Unit>()

    override var pathAndParams = window.location.hash.substring(1)
        private set

    private val popStateListener = window.disposableListener<PopStateEvent>("popstate", {
        val newPathAndParams = window.location.hash.substring(1)

        if (newPathAndParams != pathAndParams) {
            pathAndParams = newPathAndParams

            for (callback in popCallbacks) {
                callback(newPathAndParams)
            }
        }
    })

    override fun dispose() {
        popStateListener.dispose()
        super.dispose()
    }

    override fun pushPathAndParams(pathAndParams: String) {
        this.pathAndParams = pathAndParams
        window.history.pushState(null, TITLE, "$path#$pathAndParams")
    }

    override fun replacePathAndParams(pathAndParams: String) {
        this.pathAndParams = pathAndParams
        window.history.replaceState(null, TITLE, "$path#$pathAndParams")
    }

    override fun onPopPathAndParams(callback: (String) -> Unit): Disposable {
        popCallbacks.add(callback)
        return disposable { popCallbacks.remove(callback) }
    }

    companion object {
        private const val TITLE = "Phantasmal World"
    }
}
