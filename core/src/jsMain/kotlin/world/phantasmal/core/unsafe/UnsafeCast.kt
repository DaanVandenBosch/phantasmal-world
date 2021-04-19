package world.phantasmal.core.unsafe

@Suppress("NOTHING_TO_INLINE")
actual inline fun <T> T?.unsafeAssertNotNull(): T = unsafeCast<T>()
