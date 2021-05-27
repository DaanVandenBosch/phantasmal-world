package world.phantasmal.core.unsafe

/**
 * Asserts that [value] is of type T. No runtime check happens in KJS. Should only be used when it's
 * absolutely certain that [value] is indeed a T.
 */
expect inline fun <T> unsafeCast(value: Any?): T

/**
 * Asserts that [value] is not null. No runtime check happens in KJS. Should only be used when it's
 * absolutely certain that [value] is indeed not null.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T> unsafeAssertNotNull(value: T?): T = unsafeCast(value)
