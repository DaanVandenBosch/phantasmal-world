package world.phantasmal.web.core.loading

import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.decodeFromDynamic
import kotlinx.serialization.serializer
import org.khronos.webgl.ArrayBuffer
import org.w3c.fetch.Response
import world.phantasmal.core.unsafe.unsafeCast
import world.phantasmal.web.shared.JSON_FORMAT

@OptIn(ExperimentalSerializationApi::class)
class AssetLoader(
    private val origin: String = window.location.origin,
    private val basePath: String = defaultBasePath(),
) {
    suspend inline fun <reified T : Any> load(path: String): T =
        load(path, typeOf<T>())

    suspend fun <T : Any> load(path: String, type: KType) =
        JSON_FORMAT.decodeFromDynamic(
            unsafeCast<KSerializer<T>>(serializer(type)),
            get(path).json().await(),
        )

    suspend fun loadArrayBuffer(path: String): ArrayBuffer =
        get(path).arrayBuffer().await()

    private suspend fun get(path: String): Response =
        window.fetch("$origin$basePath$path").await()

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
