package world.phantasmal.lib.cursor

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.DataView

/**
 * A cursor for reading from an array buffer or part of an array buffer.
 *
 * @param buffer The buffer to read from.
 * @param endianness Decides in which byte order multi-byte integers and floats will be interpreted.
 * @param offset The start offset of the part that will be read from.
 * @param size The size of the part that will be read from.
 */
class ArrayBufferCursor(
    buffer: ArrayBuffer,
    endianness: Endianness,
    offset: UInt = 0u,
    size: UInt = buffer.byteLength.toUInt() - offset,
) : AbstractArrayBufferCursor(endianness, offset) {
    override val backingBuffer = buffer
    override val dv = DataView(buffer, 0, buffer.byteLength)

    override var size: UInt = size
        set(value) {
            require(size <= backingBuffer.byteLength.toUInt() - offset)
            field = value
        }

    override fun take(size: UInt): ArrayBufferCursor {
        val offset = offset + position
        val wrapper = ArrayBufferCursor(backingBuffer, endianness, offset, size)
        this.position += size
        return wrapper
    }
}
