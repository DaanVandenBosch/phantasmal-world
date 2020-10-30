package world.phantasmal.webui

fun <T> newJsObject(block: T.() -> Unit): T =
    js("{}").unsafeCast<T>().apply(block)
