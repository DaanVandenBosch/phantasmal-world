@file:JvmName("PrimitiveExtensionsJvm")

package world.phantasmal.core

import java.lang.Float.intBitsToFloat

actual fun Int.reinterpretAsFloat(): Float = intBitsToFloat(this)
