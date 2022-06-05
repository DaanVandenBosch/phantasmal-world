package world.phantasmal.core

inline fun assert(value: () -> Boolean) {
    assert(value) { "An assertion failed." }
}

inline fun assert(value: Boolean, lazyMessage: () -> Any) {
    assert({ value }, lazyMessage)
}

expect inline fun assert(value: () -> Boolean, lazyMessage: () -> Any)

inline fun assertUnreachable(lazyMessage: () -> Any) {
    assert({ true }, lazyMessage)
}
