package world.phantasmal.core.unsafe

actual class UnsafeMap<K, V> {
    private val map = HashMap<K, V>()

    actual fun get(key: K): V? = map[key]

    actual fun has(key: K): Boolean = key in map

    actual fun forEach(callback: (value: V, key: K) -> Unit) {
        map.forEach { (k, v) -> callback(v, k) }
    }

    actual fun set(key: K, value: V) {
        map[key] = value
    }

    actual fun delete(key: K): Boolean = map.remove(key) != null
}
