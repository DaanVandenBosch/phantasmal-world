package world.phantasmal.core

import org.w3c.dom.DOMRectReadOnly
import org.w3c.dom.Element

external interface JsArray<T> {
    val length: Int

    fun push(vararg elements: T): Int

    fun slice(start: Int = definedExternally): JsArray<T>
    fun slice(start: Int, end: Int = definedExternally): JsArray<T>

    fun some(callback: (element: T, index: Int) -> Boolean): Boolean

    fun splice(start: Int, deleteCount: Int = definedExternally): JsArray<T>
    fun splice(start: Int, deleteCount: Int, vararg items: T): JsArray<T>
}

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> JsArray<T>.get(index: Int): T = asDynamic()[index].unsafeCast<T>()

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> JsArray<T>.set(index: Int, value: T) {
    asDynamic()[index] = value
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T> jsArrayOf(vararg elements: T): JsArray<T> =
    elements.unsafeCast<JsArray<T>>()

@Suppress("NOTHING_TO_INLINE")
inline fun <T> JsArray<T>.asArray(): Array<T> =
    unsafeCast<Array<T>>()

@Suppress("NOTHING_TO_INLINE")
inline fun <T> Array<T>.asJsArray(): JsArray<T> =
    unsafeCast<JsArray<T>>()

@Suppress("NOTHING_TO_INLINE")
inline fun <T> List<T>.toJsArray(): JsArray<T> =
    toTypedArray().asJsArray()

@Suppress("unused")
external interface JsPair<out A, out B>

inline val <T> JsPair<T, *>.first: T get() = asDynamic()[0].unsafeCast<T>()
inline val <T> JsPair<*, T>.second: T get() = asDynamic()[1].unsafeCast<T>()

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> JsPair<T, *>.component1(): T = first

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> JsPair<*, T>.component2(): T = second

@JsName("Object")
external class JsObject {
    companion object {
        fun entries(jsObject: dynamic): Array<JsPair<String, dynamic>>
    }
}

external class ResizeObserver(callback: (entries: Array<ResizeObserverEntry>) -> Unit) {
    fun observe(target: Element)
    fun unobserve(target: Element)
    fun disconnect()
}

external interface ResizeObserverEntry {
    val contentRect: DOMRectReadOnly
}
