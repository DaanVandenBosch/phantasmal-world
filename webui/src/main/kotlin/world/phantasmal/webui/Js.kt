package world.phantasmal.webui

fun <T> obj(block: T.() -> Unit): T =
    js("{}").unsafeCast<T>().apply(block)
