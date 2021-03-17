package world.phantasmal.core

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
actual inline fun <T> T?.unsafeAssertNotNull(): T = this as T
