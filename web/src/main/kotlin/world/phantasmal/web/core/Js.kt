package world.phantasmal.web.core

fun <T> newJsObject(block: T.() -> Unit): T =
    js("{}").unsafeCast<T>().apply(block)
