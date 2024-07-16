package world.phantasmal.web.core.persistence

import kotlinx.browser.localStorage

interface KeyValueStore {
    suspend fun get(key: String): String?
    suspend fun put(key: String, value: String)
}

class LocalStorageKeyValueStore : KeyValueStore {
    override suspend fun get(key: String): String? =
        localStorage.getItem(key)

    override suspend fun put(key: String, value: String) {
        localStorage.setItem(key, value)
    }
}

class MemoryKeyValueStore : KeyValueStore {
    private val map = mutableMapOf<String, String>()

    override suspend fun get(key: String): String? =
        map[key]

    override suspend fun put(key: String, value: String) {
        map[key] = value
    }
}
