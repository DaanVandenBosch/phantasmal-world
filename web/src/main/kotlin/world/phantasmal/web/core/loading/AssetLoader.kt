package world.phantasmal.web.core.loading

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.khronos.webgl.ArrayBuffer

class AssetLoader(val basePath: String, val httpClient: HttpClient) {
    suspend inline fun <reified T> load(path: String): T =
        httpClient.get("$basePath$path")

    suspend fun loadArrayBuffer(path: String): ArrayBuffer {
        val response = load<HttpResponse>(path)
        val channel = response.content
        val arrayBuffer = ArrayBuffer(response.contentLength()?.toInt() ?: channel.availableForRead)
        channel.readFully(arrayBuffer, 0, arrayBuffer.byteLength)
        check(channel.availableForRead == 0) { "Couldn't read all data." }
        return arrayBuffer
    }
}
