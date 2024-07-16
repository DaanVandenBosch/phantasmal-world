package world.phantasmal.web.core.loading

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.js.*
import kotlinx.browser.window
import org.khronos.webgl.ArrayBuffer

class AssetLoader(
    private val httpClient: HttpClient,
    private val origin: String = window.location.origin,
    private val basePath: String = defaultBasePath(),
) {
    suspend inline fun <reified T : Any> load(path: String): T =
        load(path, typeInfo<T>())

    suspend fun <T : Any> load(path: String, typeInfo: TypeInfo): T =
        get(path).body(typeInfo)

    suspend fun loadArrayBuffer(path: String): ArrayBuffer =
        get(path).bodyAsChannel().readRemaining().readArrayBuffer()

    private suspend fun get(path: String): HttpResponse =
        httpClient.get("$origin$basePath$path")

    companion object {
        fun defaultBasePath(): String {
            val pathname = window.location.pathname

            val appPath =
                if (pathname.endsWith(".html")) {
                    pathname.substring(0, pathname.lastIndexOf('/'))
                } else {
                    pathname.removeSuffix("/")
                }

            return "$appPath/assets"
        }
    }
}
