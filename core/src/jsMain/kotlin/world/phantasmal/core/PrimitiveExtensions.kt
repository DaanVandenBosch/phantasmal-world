package world.phantasmal.core

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.DataView

private val dataView = DataView(ArrayBuffer(4))

actual fun Int.reinterpretAsFloat(): Float {
    dataView.setInt32(0, this)
    return dataView.getFloat32(0)
}
