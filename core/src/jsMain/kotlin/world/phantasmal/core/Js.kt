@file:Suppress("NOTHING_TO_INLINE")

package world.phantasmal.core

external interface JsArray<T> {
    val length: Int

    fun push(vararg elements: T): Int

    fun slice(start: Int = definedExternally): JsArray<T>
    fun slice(start: Int, end: Int = definedExternally): JsArray<T>

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
