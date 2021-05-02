package world.phantasmal.core.unsafe

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
actual inline fun <T> Any?.unsafeCast(): T = this as T
