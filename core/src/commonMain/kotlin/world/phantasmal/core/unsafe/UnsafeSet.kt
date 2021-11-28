package world.phantasmal.core.unsafe

/**
 * Set optimized for JS (it compiles to the built-in Set).
 *
 * In JS, values are compared by reference, equals and hashCode are NOT invoked. On JVM, equals and
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
 * 3. The values used do not require equals or hashCode to be called in JS.
 *    E.g. Int, String, objects which you consider equal if and only if they are the exact same
 *    instance.
 */
expect class UnsafeSet<T> {
    constructor()
    constructor(values: Array<out T>)

    val size: Int

    fun add(value: T): UnsafeSet<T>
    fun clear()
    fun delete(value: T): Boolean
    fun has(value: T): Boolean
    fun forEach(callback: (value: T) -> Unit)
}

/**
 * See the disclaimer at [UnsafeSet].
 */
fun <T> unsafeSetOf(vararg values: T): UnsafeSet<T> = UnsafeSet(values)
