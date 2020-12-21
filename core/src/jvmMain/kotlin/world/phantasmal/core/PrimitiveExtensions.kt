@file:JvmName("PrimitiveExtensionsJvm")

package world.phantasmal.core

import java.lang.Float.floatToIntBits
import java.lang.Float.intBitsToFloat

actual fun Int.reinterpretAsFloat(): Float = intBitsToFloat(this)

actual fun Float.reinterpretAsInt(): Int = floatToIntBits(this)
