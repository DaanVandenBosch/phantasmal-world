package world.phantasmal.core.unsafe

import kotlin.js.unsafeCast as kotlinUnsafeCast

@Suppress("NOTHING_TO_INLINE")
actual inline fun <T> Any?.unsafeCast(): T = kotlinUnsafeCast<T>()
