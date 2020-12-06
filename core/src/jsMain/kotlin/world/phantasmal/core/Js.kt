package world.phantasmal.core

@Suppress("NOTHING_TO_INLINE")
inline fun <T> jsArrayOf(vararg elements: T): JsArray<T> =
    elements.unsafeCast<JsArray<T>>()

inline fun <T> JsArray<T>.asArray(): Array<T> =
    unsafeCast<Array<T>>()
