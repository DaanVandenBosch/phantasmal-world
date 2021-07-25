package world.phantasmal.psolib.cursor

import org.khronos.webgl.Uint8Array
import world.phantasmal.psolib.Endianness

class ArrayBufferCursorTests : WritableCursorTests() {
    override fun createCursor(bytes: ByteArray, endianness: Endianness, size: Int) =
        ArrayBufferCursor(Uint8Array(bytes.toTypedArray()).buffer, endianness, size = size)
}
