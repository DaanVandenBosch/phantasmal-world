package world.phantasmal.lib.cursor

import org.khronos.webgl.Uint8Array
import world.phantasmal.lib.Endianness

class ArrayBufferCursorTests : WritableCursorTests() {
    override fun createCursor(bytes: ByteArray, endianness: Endianness) =
        ArrayBufferCursor(Uint8Array(bytes.toTypedArray()).buffer, endianness)
}
