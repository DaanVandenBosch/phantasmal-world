package world.phantasmal.core

/**
 * Asserts that T is not null. No runtime check happens in KJS. Should only be used when absolutely
 * certain that T is indeed not null.
 */
expect inline fun <T> T?.unsafeAssertNotNull(): T
