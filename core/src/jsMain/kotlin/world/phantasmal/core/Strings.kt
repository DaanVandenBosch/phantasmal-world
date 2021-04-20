package world.phantasmal.core

@Suppress("NOTHING_TO_INLINE")
actual inline fun String.getCodePointAt(index: Int): Int =
    asDynamic().charCodeAt(index).unsafeCast<Int>()
