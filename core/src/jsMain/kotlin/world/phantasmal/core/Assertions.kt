package world.phantasmal.core

import kotlin.contracts.InvocationKind.AT_MOST_ONCE
import kotlin.contracts.contract

actual inline fun assert(value: () -> Boolean, lazyMessage: () -> Any) {
    contract {
        callsInPlace(value, AT_MOST_ONCE)
        callsInPlace(lazyMessage, AT_MOST_ONCE)
    }

    // TODO: Figure out a sensible way to do dev assertions in JS.
}
