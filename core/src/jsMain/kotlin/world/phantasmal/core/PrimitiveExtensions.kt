package world.phantasmal.core

@Suppress("NOTHING_TO_INLINE")
actual inline fun Char.fastIsWhitespace(): Boolean =
    asDynamic() == 0x20 || (asDynamic() >= 0x09 && asDynamic() <= 0x0D)

@Suppress("NOTHING_TO_INLINE")
actual inline fun Char.isDigit(): Boolean =
    asDynamic() >= 0x30 && asDynamic() <= 0x39
