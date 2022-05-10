package world.phantasmal.core.unsafe

/**
 * Map optimized for JS (it compiles to the built-in Map).
 *
 * In JS, keys are compared by reference, equals and hashCode are NOT invoked. On JVM, equals and
 * hashCode ARE used.
 *
 * DO NOT USE THIS UNLESS ALL THE FOLLOWING REQUIREMENTS ARE MET:
 *
 * 1. It improves performance substantially.
 *    If it doesn't improve performance by a very noticeable amount, it's not worth the risk.
 *
 * 2. It's only used internally.
 *    E.g. in a private property which no other code can access and misuse accidentally. This way
 *    only a small part of the code can contain hard to discover errors.
 *
 * 3. The keys used do not require equals or hashCode to be called in JS.
 *    E.g. Int, String, objects which you consider equal if and only if they are the exact same
 *    instance. Note that some objects that compile to primitives on JVM, such as Long, compile to
 *    an object in JS and thus will not behave the way you expect.
 */
expect class UnsafeMap<K, V>() {
    fun get(key: K): V?
    fun has(key: K): Boolean
    fun forEach(callback: (value: V, key: K) -> Unit)
    fun set(key: K, value: V)
    fun delete(key: K): Boolean
}

inline fun <K, V : Any> UnsafeMap<K, V>.getOrPut(key: K, default: () -> V): V {
    var value = get(key)

    if (value == null) {
        value = default()
        set(key, value)
    }

    return value
}
