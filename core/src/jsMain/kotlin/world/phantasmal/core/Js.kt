@file:Suppress("NOTHING_TO_INLINE")

package world.phantasmal.core

external interface JsArray<T> {
    val length: Int

    fun push(vararg elements: T): Int

    fun slice(start: Int = definedExternally): JsArray<T>
    fun slice(start: Int, end: Int = definedExternally): JsArray<T>

    fun some(callback: (element: T, index: Int) -> Boolean): Boolean

    fun splice(start: Int, deleteCount: Int = definedExternally): JsArray<T>
    fun splice(start: Int, deleteCount: Int, vararg items: T): JsArray<T>
}

inline operator fun <T> JsArray<T>.get(index: Int): T = asDynamic()[index].unsafeCast<T>()

inline operator fun <T> JsArray<T>.set(index: Int, value: T) {
    asDynamic()[index] = value
}

inline fun <T> jsArrayOf(vararg elements: T): JsArray<T> =
    elements.unsafeCast<JsArray<T>>()

inline fun <T> JsArray<T>.asArray(): Array<T> =
    unsafeCast<Array<T>>()

inline fun <T> Array<T>.asJsArray(): JsArray<T> =
    unsafeCast<JsArray<T>>()

inline fun <T> List<T>.toJsArray(): JsArray<T> =
    toTypedArray().asJsArray()

@Suppress("unused")
external interface JsPair<out A, out B>

inline val <T> JsPair<T, *>.first: T get() = asDynamic()[0].unsafeCast<T>()
inline val <T> JsPair<*, T>.second: T get() = asDynamic()[1].unsafeCast<T>()

inline operator fun <T> JsPair<T, *>.component1(): T = first
inline operator fun <T> JsPair<*, T>.component2(): T = second

@Suppress("FunctionName", "UNUSED_PARAMETER")
inline fun <A, B> JsPair(first: A, second: B): JsPair<A, B> =
    js("[first, second]").unsafeCast<JsPair<A, B>>()

@Suppress("UNUSED_PARAMETER")
inline fun objectEntries(jsObject: dynamic): Array<JsPair<String, dynamic>> =
    js("Object.entries(jsObject)").unsafeCast<Array<JsPair<String, dynamic>>>()

external interface JsSet<T> {
    val size: Int

    fun add(value: T): JsSet<T>
    fun clear()
    fun delete(value: T): Boolean
    fun has(value: T): Boolean
    fun forEach(callback: (value: T) -> Unit)
}

inline fun <T> emptyJsSet(): JsSet<T> =
    js("new Set()").unsafeCast<JsSet<T>>()

@Suppress("UNUSED_PARAMETER")
inline fun <T> jsSetOf(vararg values: T): JsSet<T> =
    js("new Set(values)").unsafeCast<JsSet<T>>()

external interface JsMap<K, V> {
    val size: Int

    fun clear()
    fun delete(key: K): Boolean
    fun forEach(callback: (value: V, key: K) -> Unit)
    fun get(key: K): V?
    fun has(key: K): Boolean
    fun set(key: K, value: V): JsMap<K, V>
}

@Suppress("UNUSED_PARAMETER")
inline fun <K, V> jsMapOf(vararg pairs: JsPair<K, V>): JsMap<K, V> =
    js("new Map(pairs)").unsafeCast<JsMap<K, V>>()

inline fun <K, V> emptyJsMap(): JsMap<K, V> =
    js("new Map()").unsafeCast<JsMap<K, V>>()
