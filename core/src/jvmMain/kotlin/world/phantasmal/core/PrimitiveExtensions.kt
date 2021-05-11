@file:JvmName("PrimitiveExtensionsJvm")

package world.phantasmal.core

@Suppress("NOTHING_TO_INLINE")
actual inline fun Char.fastIsWhitespace(): Boolean = isWhitespace()

@Suppress("NOTHING_TO_INLINE")
actual inline fun Char.isDigit(): Boolean = this in '0'..'9'
