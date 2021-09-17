package world.phantasmal.core.unsafe

/**
 * Map optimized for JS (it compiles to the built-in Map).
 * In JS, keys are compared by reference, equals and hashCode are NOT invoked. On JVM, equals and
 * hashCode ARE used.
 */
expect class UnsafeMap<K, V>() {
    fun get(key: K): V?
    fun has(key: K): Boolean
    fun forEach(callback: (value: V, key: K) -> Unit)
    fun set(key: K, value: V)
    fun delete(key: K): Boolean
}

fun <K, V : Any> UnsafeMap<K, V>.getOrPut(key: K, default: () -> V): V {
    var value = get(key)

    if (value == null) {
        value = default()
        set(key, value)
    }

    return value
}
