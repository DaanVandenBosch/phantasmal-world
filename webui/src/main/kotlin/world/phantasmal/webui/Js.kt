package world.phantasmal.webui

import org.w3c.files.File
import world.phantasmal.core.filenameExtension

inline fun <T> obj(block: T.() -> Unit): T =
    js("{}").unsafeCast<T>().apply(block)

fun File.extension(): String? =
    filenameExtension(name)
