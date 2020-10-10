package world.phantasmal.core

actual fun <T> Any?.fastCast(): T = unsafeCast<T>()
