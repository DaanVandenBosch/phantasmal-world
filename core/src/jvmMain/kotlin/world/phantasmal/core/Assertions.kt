@file:JvmName("AssertionsJvm")

package world.phantasmal.core

val ASSERTIONS_ENABLED: Boolean = {}.javaClass.desiredAssertionStatus()

actual inline fun assert(value: () -> Boolean, lazyMessage: () -> Any) {
    if (ASSERTIONS_ENABLED && !value()) {
        throw AssertionError(lazyMessage())
    }
}
