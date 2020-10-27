package world.phantasmal.core

@Suppress("UNCHECKED_CAST")
actual fun <T> T?.unsafeToNonNull(): T = this as T
