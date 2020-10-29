package world.phantasmal.lib.cursor

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.DataView
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.buffer.Buffer

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
    offset: Int = 0,
    size: Int = buffer.byteLength - offset,
) : AbstractWritableCursor(offset) {
    private var littleEndian: Boolean = endianness == Endianness.Little
    private val backingBuffer = buffer
    private val dv = DataView(buffer)

    override var size: Int = size
        set(value) {
            require(size <= backingBuffer.byteLength - offset)
            field = value
        }

    override var endianness: Endianness
        get() = if (littleEndian) Endianness.Little else Endianness.Big
        set(value) {
            littleEndian = value == Endianness.Little
        }

    override fun uByte(): UByte {
        requireSize(1)
        val r = dv.getUint8(absolutePosition)
        position++
        return r.toUByte()
    }

    override fun uShort(): UShort {
        requireSize(2)
        val r = dv.getUint16(absolutePosition, littleEndian)
        position += 2
        return r.toUShort()
    }

    override fun uInt(): UInt {
        requireSize(4)
        val r = dv.getUint32(absolutePosition, littleEndian)
        position += 4
        return r.toUInt()
    }

    override fun byte(): Byte {
        requireSize(1)
        val r = dv.getInt8(absolutePosition)
        position++
        return r
    }

    override fun short(): Short {
        requireSize(2)
        val r = dv.getInt16(absolutePosition, littleEndian)
        position += 2
        return r
    }

    override fun int(): Int {
        requireSize(4)
        val r = dv.getInt32(absolutePosition, littleEndian)
        position += 4
        return r
    }

    override fun float(): Float {
        requireSize(4)
        val r = dv.getFloat32(absolutePosition, littleEndian)
        position += 4
        return r
    }

    override fun uByteArray(n: Int): UByteArray {
        requireSize(n)

        val array = UByteArray(n)

        for (i in 0 until n) {
            array[i] = dv.getUint8(absolutePosition).toUByte()
            position++
        }

        return array
    }

    override fun uShortArray(n: Int): UShortArray {
        requireSize(2 * n)

        val array = UShortArray(n)

        for (i in 0 until n) {
            array[i] = dv.getUint16(absolutePosition, littleEndian).toUShort()
            position += 2
        }

        return array
    }

    override fun uIntArray(n: Int): UIntArray {
        requireSize(4 * n)

        val array = UIntArray(n)

        for (i in 0 until n) {
            array[i] = dv.getUint32(absolutePosition, littleEndian).toUInt()
            position += 4
        }

        return array
    }

    override fun byteArray(n: Int): ByteArray {
        requireSize(n)

        val array = ByteArray(n)

        for (i in 0 until n) {
            array[i] = dv.getInt8(absolutePosition)
            position++
        }

        return array
    }

    override fun intArray(n: Int): IntArray {
        requireSize(4 * n)

        val array = IntArray(n)

        for (i in 0 until n) {
            array[i] = dv.getInt32(absolutePosition, littleEndian)
            position += 4
        }

        return array
    }

    override fun take(size: Int): Cursor {
        val offset = offset + position
        val wrapper = ArrayBufferCursor(backingBuffer, endianness, offset, size)
        this.position += size
        return wrapper
    }

    override fun buffer(size: Int): Buffer {
        requireSize(size)
        val r = Buffer.fromArrayBuffer(
            backingBuffer.slice(absolutePosition, (absolutePosition + size)),
            endianness
        )
        position += size
        return r
    }

    override fun writeUByte(value: UByte): WritableCursor {
        requireSize(1)
        dv.setUint8(absolutePosition, value.toByte())
        position++
        return this
    }

    override fun writeUShort(value: UShort): WritableCursor {
        requireSize(2)
        dv.setUint16(absolutePosition, value.toShort(), littleEndian)
        position += 2
        return this
    }

    override fun writeUInt(value: UInt): WritableCursor {
        requireSize(4)
        dv.setUint32(absolutePosition, value.toInt(), littleEndian)
        position += 4
        return this
    }

    override fun writeByte(value: Byte): WritableCursor {
        requireSize(1)
        dv.setInt8(absolutePosition, value)
        position++
        return this
    }

    override fun writeShort(value: Short): WritableCursor {
        requireSize(2)
        dv.setInt16(absolutePosition, value, littleEndian)
        position += 2
        return this
    }

    override fun writeInt(value: Int): WritableCursor {
        requireSize(4)
        dv.setInt32(absolutePosition, value, littleEndian)
        position += 4
        return this
    }

    override fun writeFloat(value: Float): WritableCursor {
        requireSize(4)
        dv.setFloat32(absolutePosition, value, littleEndian)
        position += 4
        return this
    }
}

fun ArrayBuffer.cursor(endianness: Endianness): ArrayBufferCursor =
    ArrayBufferCursor(this, endianness)
