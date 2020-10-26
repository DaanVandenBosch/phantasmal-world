package world.phantasmal.lib.cursor

import world.phantasmal.lib.Endianness
import world.phantasmal.lib.buffer.Buffer

/**
 * @param buffer The Buffer to read from and write to.
 * @param offset The start offset of the part that will be read from.
 * @param size The size of the part that will be read from.
 */
class BufferCursor(
    private val buffer: Buffer,
    offset: Int = 0,
    size: Int = buffer.size - offset,
) : AbstractWritableCursor(offset) {
    private var _size = size

    override var size: Int
        get() = _size
        set(value) {
            if (value > _size) {
                ensureSpace(value)
            } else {
                _size = value
            }
        }

    /**
     * Mirrors the underlying buffer's endianness.
     */
    override var endianness: Endianness
        get() = buffer.endianness
        set(value) {
            buffer.endianness = value
        }

    init {
        require(offset <= buffer.size) {
            "Offset $offset is out of bounds."
        }

        require(offset + size <= buffer.size) {
            "Size $size is out of bounds."
        }
    }

    override fun u8(): UByte {
        val r = buffer.getU8(absolutePosition)
        position++
        return r
    }

    override fun u16(): UShort {
        val r = buffer.getU16(absolutePosition)
        position += 2
        return r
    }

    override fun u32(): UInt {
        val r = buffer.getU32(absolutePosition)
        position += 4
        return r
    }

    override fun i8(): Byte {
        val r = buffer.getI8(absolutePosition)
        position++
        return r
    }

    override fun i16(): Short {
        val r = buffer.getI16(absolutePosition)
        position += 2
        return r
    }

    override fun i32(): Int {
        val r = buffer.getI32(absolutePosition)
        position += 4
        return r
    }

    override fun f32(): Float {
        val r = buffer.getF32(absolutePosition)
        position += 4
        return r
    }

    override fun u8Array(n: Int): UByteArray {
        requireSize(n)

        val array = UByteArray(n)

        for (i in 0 until n) {
            array[i] = buffer.getU8(absolutePosition)
            position++
        }

        return array
    }

    override fun u16Array(n: Int): UShortArray {
        requireSize(2 * n)

        val array = UShortArray(n)

        for (i in 0 until n) {
            array[i] = buffer.getU16(absolutePosition)
            position += 2
        }

        return array
    }

    override fun u32Array(n: Int): UIntArray {
        requireSize(4 * n)

        val array = UIntArray(n)

        for (i in 0 until n) {
            array[i] = buffer.getU32(absolutePosition)
            position += 4
        }

        return array
    }

    override fun i32Array(n: Int): IntArray {
        requireSize(4 * n)

        val array = IntArray(n)

        for (i in 0 until n) {
            array[i] = buffer.getI32(absolutePosition)
            position += 4
        }

        return array
    }

    override fun take(size: Int): Cursor {
        val wrapper = BufferCursor(buffer, offset = absolutePosition, size)
        position += size
        return wrapper
    }

    override fun buffer(size: Int): Buffer {
        val wrapper = buffer.slice(offset = absolutePosition, size)
        position += size
        return wrapper
    }

    override fun writeU8(value: UByte): WritableCursor {
        ensureSpace(1)
        buffer.setU8(absolutePosition, value)
        position++
        return this
    }

    override fun writeU16(value: UShort): WritableCursor {
        ensureSpace(2)
        buffer.setU16(absolutePosition, value)
        position += 2
        return this
    }

    override fun writeU32(value: UInt): WritableCursor {
        ensureSpace(4)
        buffer.setU32(absolutePosition, value)
        position += 4
        return this
    }

    override fun writeI8(value: Byte): WritableCursor {
        ensureSpace(1)
        buffer.setI8(absolutePosition, value)
        position++
        return this
    }

    override fun writeI16(value: Short): WritableCursor {
        ensureSpace(2)
        buffer.setI16(absolutePosition, value)
        position += 2
        return this
    }

    override fun writeI32(value: Int): WritableCursor {
        ensureSpace(4)
        buffer.setI32(absolutePosition, value)
        position += 4
        return this
    }

    override fun writeF32(value: Float): WritableCursor {
        ensureSpace(4)
        buffer.setF32(absolutePosition, value)
        position += 4
        return this
    }

    override fun writeU8Array(array: UByteArray): WritableCursor {
        ensureSpace(array.size)
        return super.writeU8Array(array)
    }

    override fun writeU16Array(array: UShortArray): WritableCursor {
        ensureSpace(2 * array.size)
        return super.writeU16Array(array)
    }

    override fun writeU32Array(array: UIntArray): WritableCursor {
        ensureSpace(4 * array.size)
        return super.writeU32Array(array)
    }

    override fun writeI32Array(array: IntArray): WritableCursor {
        ensureSpace(4 * array.size)
        return super.writeI32Array(array)
    }

    override fun writeCursor(other: Cursor): WritableCursor {
        val size = other.size - other.position
        ensureSpace(size)
        return super.writeCursor(other)
    }

    override fun writeStringAscii(str: String, byteLength: Int): WritableCursor {
        ensureSpace(byteLength)
        return super.writeStringAscii(str, byteLength)
    }

    override fun writeStringUtf16(str: String, byteLength: Int): WritableCursor {
        ensureSpace(byteLength)
        return super.writeStringUtf16(str, byteLength)
    }

    private fun ensureSpace(size: Int) {
        val needed = (position + size) - _size

        if (needed > 0) {
            _size += needed

            if (buffer.size < offset + _size) {
                buffer.size = offset + _size
            }
        }
    }
}

fun Buffer.cursor(): BufferCursor =
    BufferCursor(this)
