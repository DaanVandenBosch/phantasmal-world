package world.phantasmal.core

inline fun assert(value: () -> Boolean) {
    assert(value) { "An assertion failed." }
}

expect inline fun assert(value: () -> Boolean, lazyMessage: () -> Any)
