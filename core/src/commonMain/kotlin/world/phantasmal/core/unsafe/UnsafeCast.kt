package world.phantasmal.core.unsafe

/**
 * Asserts that receiver is of type T. No runtime check happens in KJS. Should only be used when
 * absolutely certain that receiver is indeed a T.
 */
expect inline fun <T> Any?.unsafeCast(): T

/**
 * Asserts that T is not null. No runtime check happens in KJS. Should only be used when absolutely
 * certain that T is indeed not null.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T> T?.unsafeAssertNotNull(): T = unsafeCast()
