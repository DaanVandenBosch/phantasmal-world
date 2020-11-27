package world.phantasmal.web.core.persistence

import kotlinx.browser.localStorage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import mu.KotlinLogging
import world.phantasmal.web.core.models.Server

private val logger = KotlinLogging.logger {}

abstract class Persister {
    private val format = Json {
        classDiscriminator = "#type"
        ignoreUnknownKeys = true
    }

    protected suspend inline fun <reified T> persist(key: String, data: T) {
        persist(key, data, serializer())
    }

    protected suspend fun <T> persist(key: String, data: T, serializer: KSerializer<T>) {
        try {
            localStorage.setItem(key, format.encodeToString(serializer, data))
        } catch (e: Throwable) {
            logger.error(e) { "Couldn't persist ${key}." }
        }
    }

    protected suspend fun persistForServer(server: Server, key: String, data: Any) {
        persist(serverKey(server, key), data)
    }

    protected suspend inline fun <reified T> load(key: String): T? =
        load(key, serializer())

    protected suspend fun <T> load(key: String, serializer: KSerializer<T>): T? =
        try {
            val json = localStorage.getItem(key)
            json?.let { format.decodeFromString(serializer, it) }
        } catch (e: Throwable) {
            logger.error(e) { "Couldn't load ${key}." }
            null
        }

    protected suspend inline fun <reified T> loadForServer(server: Server, key: String): T? =
        load(serverKey(server, key))

    fun serverKey(server: Server, key: String): String {
        // Do this manually per server type instead of just appending e.g. `server` to ensure the
        // persisted key never changes.
        val serverKey = when (server) {
            Server.Ephinea -> "Ephinea"
        }

        return "$key.$serverKey"
    }
}
