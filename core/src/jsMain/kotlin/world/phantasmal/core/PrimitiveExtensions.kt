package world.phantasmal.core

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.DataView

private val dataView = DataView(ArrayBuffer(4))

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
