@file:JvmName("PrimitiveExtensionsJvm")

package world.phantasmal.core

import java.lang.Float.floatToIntBits
import java.lang.Float.intBitsToFloat

@Suppress("NOTHING_TO_INLINE")
actual inline fun Char.fastIsWhitespace(): Boolean = isWhitespace()

@Suppress("NOTHING_TO_INLINE")
actual inline fun Char.isDigit(): Boolean = this in '0'..'9'

actual fun Int.reinterpretAsFloat(): Float = intBitsToFloat(this)

actual fun Float.reinterpretAsInt(): Int = floatToIntBits(this)

actual fun Float.reinterpretAsUInt(): UInt = reinterpretAsInt().toUInt()
