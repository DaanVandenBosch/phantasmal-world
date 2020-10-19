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
    offset: UInt = 0u,
    size: UInt = buffer.size - offset,
) : AbstractWritableCursor(offset) {
    private var _size = size

    override var size: UInt
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
        position += 2u
        return r
    }

    override fun u32(): UInt {
        val r = buffer.getU32(absolutePosition)
        position += 4u
        return r
    }

    override fun i8(): Byte {
        val r = buffer.getI8(absolutePosition)
        position++
        return r
    }

    override fun i16(): Short {
        val r = buffer.getI16(absolutePosition)
        position += 2u
        return r
    }

    override fun i32(): Int {
        val r = buffer.getI32(absolutePosition)
        position += 4u
        return r
    }

    override fun f32(): Float {
        val r = buffer.getF32(absolutePosition)
        position += 4u
        return r
    }

    override fun u8Array(n: UInt): UByteArray {
        requireSize(n)

        val array = UByteArray(n.toInt())

        for (i in 0 until n.toInt()) {
            array[i] = buffer.getU8(absolutePosition)
            position++
        }

        return array
    }

    override fun u16Array(n: UInt): UShortArray {
        requireSize(2u * n)

        val array = UShortArray(n.toInt())

        for (i in 0 until n.toInt()) {
            array[i] = buffer.getU16(absolutePosition)
            position += 2u
        }

        return array
    }

    override fun u32Array(n: UInt): UIntArray {
        requireSize(4u * n)

        val array = UIntArray(n.toInt())

        for (i in 0 until n.toInt()) {
            array[i] = buffer.getU32(absolutePosition)
            position += 4u
        }

        return array
    }

    override fun i32Array(n: UInt): IntArray {
        requireSize(4u * n)

        val array = IntArray(n.toInt())

        for (i in 0 until n.toInt()) {
            array[i] = buffer.getI32(absolutePosition)
            position += 4u
        }

        return array
    }

    override fun take(size: UInt): Cursor {
        val wrapper = BufferCursor(buffer, offset = absolutePosition, size)
        position += size
        return wrapper
    }

    override fun buffer(size: UInt): Buffer {
        val wrapper = buffer.slice(offset = absolutePosition, size)
        position += size
        return wrapper
    }

    override fun writeU8(value: UByte): WritableCursor {
        ensureSpace(1u)
        buffer.setU8(absolutePosition, value)
        position++
        return this
    }

    override fun writeU16(value: UShort): WritableCursor {
        ensureSpace(2u)
        buffer.setU16(absolutePosition, value)
        position += 2u
        return this
    }

    override fun writeU32(value: UInt): WritableCursor {
        ensureSpace(4u)
        buffer.setU32(absolutePosition, value)
        position += 4u
        return this
    }

    override fun writeI8(value: Byte): WritableCursor {
        ensureSpace(1u)
        buffer.setI8(absolutePosition, value)
        position++
        return this
    }

    override fun writeI16(value: Short): WritableCursor {
        ensureSpace(2u)
        buffer.setI16(absolutePosition, value)
        position += 2u
        return this
    }

    override fun writeI32(value: Int): WritableCursor {
        ensureSpace(4u)
        buffer.setI32(absolutePosition, value)
        position += 4u
        return this
    }

    override fun writeF32(value: Float): WritableCursor {
        ensureSpace(4u)
        buffer.setF32(absolutePosition, value)
        position += 4u
        return this
    }

    override fun writeU8Array(array: UByteArray): WritableCursor {
        ensureSpace(array.size.toUInt())
        return super.writeU8Array(array)
    }

    override fun writeU16Array(array: UShortArray): WritableCursor {
        ensureSpace(2u * array.size.toUInt())
        return super.writeU16Array(array)
    }

    override fun writeU32Array(array: UIntArray): WritableCursor {
        ensureSpace(4u * array.size.toUInt())
        return super.writeU32Array(array)
    }

    override fun writeI32Array(array: IntArray): WritableCursor {
        ensureSpace(4u * array.size.toUInt())
        return super.writeI32Array(array)
    }

    override fun writeCursor(other: Cursor): WritableCursor {
        val size = other.size - other.position
        ensureSpace(size)
        return super.writeCursor(other)
    }

    override fun writeStringAscii(str: String, byteLength: UInt): WritableCursor {
        ensureSpace(byteLength)
        return super.writeStringAscii(str, byteLength)
    }

    override fun writeStringUtf16(str: String, byteLength: UInt): WritableCursor {
        ensureSpace(byteLength)
        return super.writeStringUtf16(str, byteLength)
    }

    private fun ensureSpace(size: UInt) {
        val needed = (position + size).toInt() - _size.toInt()

        if (needed > 0) {
            _size += needed.toUInt()

            if (buffer.size < offset + _size) {
                buffer.size = offset + _size
            }
        }
    }
}
