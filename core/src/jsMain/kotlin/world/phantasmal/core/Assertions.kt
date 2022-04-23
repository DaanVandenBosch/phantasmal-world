package world.phantasmal.core

actual inline fun assert(value: () -> Boolean, lazyMessage: () -> Any) {
    // TODO: Figure out a sensible way to do dev assertions in JS.
}
