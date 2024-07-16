package world.phantasmal.web.core.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import mu.KotlinLogging
import world.phantasmal.web.core.models.Server

private val logger = KotlinLogging.logger {}

abstract class Persister(private val store: KeyValueStore) {
    private val format = Json {
        classDiscriminator = "#type"
        ignoreUnknownKeys = true
    }

    protected suspend inline fun <reified T> persist(key: String, data: T) {
        persist(key, data, serializer())
    }

    // Method suspends so we can use async storage in the future.
    @Suppress("RedundantSuspendModifier")
    protected suspend fun <T> persist(key: String, data: T, serializer: KSerializer<T>) {
        withContext(Dispatchers.Default) {
            logger.trace { """Persisting data with key "$key".""" }

            try {
                store.put(key, format.encodeToString(serializer, data))
            } catch (e: Throwable) {
                logger.error(e) { """Couldn't persist data with key "$key".""" }
            }
        }
    }

    protected suspend inline fun <reified T> persistForServer(
        server: Server,
        key: String,
        data: T,
    ) {
        persist(serverKey(server, key), data)
    }

    protected suspend inline fun <reified T> load(key: String): T? =
        load(key, serializer())

    // Method suspends so we can use async storage in the future.
    @Suppress("RedundantSuspendModifier")
    protected suspend fun <T> load(key: String, serializer: KSerializer<T>): T? =
        withContext(Dispatchers.Default) {
            logger.trace { """Loading persisted data with key "$key".""" }

            try {
                val json = store.get(key)

                if (json == null) {
                    logger.trace { """No persisted data with key "$key".""" }
                    null
                } else {
                    logger.trace { """Loaded persisted data with key "$key".""" }
                    format.decodeFromString(serializer, json)
                }
            } catch (e: Throwable) {
                logger.error(e) { """Couldn't load persisted data with key "$key".""" }
                null
            }
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
