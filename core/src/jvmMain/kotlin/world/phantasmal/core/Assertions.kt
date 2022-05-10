@file:JvmName("AssertionsJvm")

package world.phantasmal.core

import kotlin.contracts.InvocationKind.AT_MOST_ONCE
import kotlin.contracts.contract

val ASSERTIONS_ENABLED: Boolean = {}.javaClass.desiredAssertionStatus()

actual inline fun assert(value: () -> Boolean, lazyMessage: () -> Any) {
    contract {
        callsInPlace(value, AT_MOST_ONCE)
        callsInPlace(lazyMessage, AT_MOST_ONCE)
    }

    if (ASSERTIONS_ENABLED && !value()) {
        throw AssertionError(lazyMessage())
    }
}
