package world.phantasmal.core

actual fun <T> T?.unsafeToNonNull(): T = unsafeCast<T>()
