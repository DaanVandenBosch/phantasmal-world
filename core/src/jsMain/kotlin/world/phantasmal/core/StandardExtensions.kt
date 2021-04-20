package world.phantasmal.core

@Suppress("NOTHING_TO_INLINE")
actual inline fun String.fastReplace(oldValue: String, newValue: String): String =
    asDynamic().replaceAll(oldValue, newValue).unsafeCast<String>()
