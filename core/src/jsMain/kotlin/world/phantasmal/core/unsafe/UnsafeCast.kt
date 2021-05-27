package world.phantasmal.core.unsafe

@Suppress("NOTHING_TO_INLINE")
actual inline fun <T> unsafeCast(value: Any?): T = value.unsafeCast<T>()
