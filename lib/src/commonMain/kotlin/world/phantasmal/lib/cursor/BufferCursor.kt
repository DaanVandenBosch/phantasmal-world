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
        require(offset in 0..buffer.size) {
            "Offset $offset is out of bounds."
        }

        require(size >= 0 && offset + size <= buffer.size) {
            "Size $size is out of bounds."
        }
    }

    override fun uByte(): UByte {
        val r = buffer.getUByte(absolutePosition)
        position++
        return r
    }

    override fun uShort(): UShort {
        val r = buffer.getUShort(absolutePosition)
        position += 2
        return r
    }

    override fun uInt(): UInt {
        val r = buffer.getUInt(absolutePosition)
        position += 4
        return r
    }

    override fun byte(): Byte {
        val r = buffer.getByte(absolutePosition)
        position++
        return r
    }

    override fun short(): Short {
        val r = buffer.getShort(absolutePosition)
        position += 2
        return r
    }

    override fun int(): Int {
        val r = buffer.getInt(absolutePosition)
        position += 4
        return r
    }

    override fun float(): Float {
        val r = buffer.getFloat(absolutePosition)
        position += 4
        return r
    }

    override fun uByteArray(n: Int): UByteArray {
        requireSize(n)

        val array = UByteArray(n)

        for (i in 0 until n) {
            array[i] = buffer.getUByte(absolutePosition)
            position++
        }

        return array
    }

    override fun uShortArray(n: Int): UShortArray {
        requireSize(2 * n)

        val array = UShortArray(n)

        for (i in 0 until n) {
            array[i] = buffer.getUShort(absolutePosition)
            position += 2
        }

        return array
    }

    override fun uIntArray(n: Int): UIntArray {
        requireSize(4 * n)

        val array = UIntArray(n)

        for (i in 0 until n) {
            array[i] = buffer.getUInt(absolutePosition)
            position += 4
        }

        return array
    }

    override fun byteArray(n: Int): ByteArray {
        requireSize(n)

        val array = ByteArray(n)

        for (i in 0 until n) {
            array[i] = buffer.getByte(absolutePosition)
            position++
        }

        return array
    }

    override fun intArray(n: Int): IntArray {
        requireSize(4 * n)

        val array = IntArray(n)

        for (i in 0 until n) {
            array[i] = buffer.getInt(absolutePosition)
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

    override fun writeUByte(value: UByte): WritableCursor {
        ensureSpace(1)
        buffer.setUByte(absolutePosition, value)
        position++
        return this
    }

    override fun writeUShort(value: UShort): WritableCursor {
        ensureSpace(2)
        buffer.setUShort(absolutePosition, value)
        position += 2
        return this
    }

    override fun writeUInt(value: UInt): WritableCursor {
        ensureSpace(4)
        buffer.setUInt(absolutePosition, value)
        position += 4
        return this
    }

    override fun writeByte(value: Byte): WritableCursor {
        ensureSpace(1)
        buffer.setByte(absolutePosition, value)
        position++
        return this
    }

    override fun writeShort(value: Short): WritableCursor {
        ensureSpace(2)
        buffer.setShort(absolutePosition, value)
        position += 2
        return this
    }

    override fun writeInt(value: Int): WritableCursor {
        ensureSpace(4)
        buffer.setInt(absolutePosition, value)
        position += 4
        return this
    }

    override fun writeFloat(value: Float): WritableCursor {
        ensureSpace(4)
        buffer.setFloat(absolutePosition, value)
        position += 4
        return this
    }

    override fun writeUByteArray(array: UByteArray): WritableCursor {
        ensureSpace(array.size)
        return super.writeUByteArray(array)
    }

    override fun writeUShortArray(array: UShortArray): WritableCursor {
        ensureSpace(2 * array.size)
        return super.writeUShortArray(array)
    }

    override fun writeUIntArray(array: UIntArray): WritableCursor {
        ensureSpace(4 * array.size)
        return super.writeUIntArray(array)
    }

    override fun writeByteArray(array: ByteArray): WritableCursor {
        ensureSpace(array.size)
        return super.writeByteArray(array)
    }

    override fun writeIntArray(array: IntArray): WritableCursor {
        ensureSpace(4 * array.size)
        return super.writeIntArray(array)
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
