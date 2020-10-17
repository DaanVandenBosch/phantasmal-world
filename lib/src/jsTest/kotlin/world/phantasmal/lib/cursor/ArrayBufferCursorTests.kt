package world.phantasmal.lib.cursor

import org.khronos.webgl.Uint8Array

class ArrayBufferCursorTests : WritableCursorTests() {
    override fun createCursor(bytes: Array<Byte>, endianness: Endianness) =
        ArrayBufferCursor(Uint8Array(bytes).buffer, endianness)
}
