package world.phantasmal.core

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.DataView

private val dataView = DataView(ArrayBuffer(4))

@Suppress("NOTHING_TO_INLINE")
actual inline fun Char.fastIsWhitespace(): Boolean =
    asDynamic() == 0x20 || (asDynamic() >= 0x09 && asDynamic() <= 0x0D)

@Suppress("NOTHING_TO_INLINE")
actual inline fun Char.isDigit(): Boolean =
    asDynamic() >= 0x30 && asDynamic() <= 0x39

actual fun Int.reinterpretAsFloat(): Float {
    dataView.setInt32(0, this)
    return dataView.getFloat32(0)
}

actual fun Float.reinterpretAsInt(): Int {
    dataView.setFloat32(0, this)
    return dataView.getInt32(0)
}

actual fun Float.reinterpretAsUInt(): UInt {
    dataView.setFloat32(0, this)
    return dataView.getUint32(0).toUInt()
}
