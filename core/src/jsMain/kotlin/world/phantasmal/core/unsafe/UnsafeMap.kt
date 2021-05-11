package world.phantasmal.core.unsafe

@JsName("Map")
actual external class UnsafeMap<K, V> {
    actual fun get(key: K): V?
    actual fun has(key: K): Boolean
    actual fun forEach(callback: (value: V, key: K) -> Unit)
    actual fun set(key: K, value: V)
    actual fun delete(key: K): Boolean
}
