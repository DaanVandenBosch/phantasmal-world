package world.phantasmal.web.core.loading

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.khronos.webgl.ArrayBuffer
import world.phantasmal.web.shared.dto.QuestDto

class AssetLoader(
    val httpClient: HttpClient,
    val origin: String = window.location.origin,
    val basePath: String = defaultBasePath(),
) {
    suspend inline fun <reified T> load(path: String): T =
        httpClient.get("$origin$basePath$path")

    suspend fun loadArrayBuffer(path: String): ArrayBuffer {
        val response = load<HttpResponse>(path)
        val channel = response.content
        val arrayBuffer = ArrayBuffer(response.contentLength()?.toInt() ?: channel.availableForRead)
        channel.readFully(arrayBuffer, 0, arrayBuffer.byteLength)
        check(channel.availableForRead == 0) { "Couldn't read all data." }
        return arrayBuffer
    }

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
