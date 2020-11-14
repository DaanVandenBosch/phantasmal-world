package world.phantasmal.webui

inline fun <T> obj(block: T.() -> Unit): T =
    js("{}").unsafeCast<T>().apply(block)
