package world.phantasmal.psolib.buffer

import world.phantasmal.psolib.Endianness
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.math.min

actual class Buffer private constructor(
    private var buf: ByteBuffer,
    size: Int,
    endianness: Endianness,
) {
    actual var size: Int = size
        set(value) {
            ensureCapacity(value)
            field = value
        }

    actual var endianness: Endianness
        get() = if (buf.order() == ByteOrder.LITTLE_ENDIAN) Endianness.Little else Endianness.Big
        set(value) {
            buf.order(
                if (value == Endianness.Little) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
            )
        }

    actual val capacity: Int
        get() = buf.capacity()

    /**
     * The backing byte array. Changes to this array will be reflected by the buffer.
     */
    val byteArray: ByteArray get() = buf.array()

    init {
        this.endianness = endianness
    }

    actual fun getUByte(offset: Int): UByte {
        checkOffset(offset, 1)
        return buf.get(offset).toUByte()
    }

    actual fun getUShort(offset: Int): UShort {
        checkOffset(offset, 2)
        return buf.getShort(offset).toUShort()
    }

    actual fun getUInt(offset: Int): UInt {
        checkOffset(offset, 4)
        return buf.getInt(offset).toUInt()
    }

    actual fun getByte(offset: Int): Byte {
        checkOffset(offset, 1)
        return buf.get(offset)
    }

    actual fun getShort(offset: Int): Short {
        checkOffset(offset, 2)
        return buf.getShort(offset)
    }

    actual fun getInt(offset: Int): Int {
        checkOffset(offset, 4)
        return buf.getInt(offset)
    }

    actual fun getFloat(offset: Int): Float {
        checkOffset(offset, 4)
        return buf.getFloat(offset)
    }

    actual fun getStringAscii(
        offset: Int,
        maxByteLength: Int,
        nullTerminated: Boolean,
    ): String =
        buildString {
            for (i in 0 until maxByteLength) {
                val codePoint = (buf.get(offset + i).toInt() and 0xFF).toChar()

                if (nullTerminated && codePoint == '\u0000') {
                    break
                }

                append(codePoint)
            }
        }

    actual fun getStringUtf16(
        offset: Int,
        maxByteLength: Int,
        nullTerminated: Boolean,
    ): String =
        buildString {
            val len = maxByteLength / 2

            for (i in 0 until len) {
                val codePoint = buf.getChar(offset + i * 2)

                if (nullTerminated && codePoint == '0') {
                    break
                }

                append(codePoint)
            }
        }

    actual fun slice(offset: Int, size: Int): Buffer {
        checkOffset(offset, size)
        return fromByteArray(
            buf.array().copyInto(ByteArray(size), 0, offset, (offset + size)),
            endianness
        )
    }

    actual fun setUByte(offset: Int, value: UByte): Buffer {
        checkOffset(offset, 1)
        buf.put(offset, value.toByte())
        return this
    }

    actual fun setUShort(offset: Int, value: UShort): Buffer {
        checkOffset(offset, 2)
        buf.putShort(offset, value.toShort())
        return this
    }

    actual fun setUInt(offset: Int, value: UInt): Buffer {
        checkOffset(offset, 4)
        buf.putInt(offset, value.toInt())
        return this
    }

    actual fun setByte(offset: Int, value: Byte): Buffer {
        checkOffset(offset, 1)
        buf.put(offset, value)
        return this
    }

    actual fun setShort(offset: Int, value: Short): Buffer {
        checkOffset(offset, 2)
        buf.putShort(offset, value)
        return this
    }

    actual fun setInt(offset: Int, value: Int): Buffer {
        checkOffset(offset, 4)
        buf.putInt(offset, value)
        return this
    }

    actual fun setFloat(offset: Int, value: Float): Buffer {
        checkOffset(offset, 4)
        buf.putFloat(offset, value)
        return this
    }

    actual fun setStringAscii(offset: Int, str: String, byteLength: Int): Buffer {
        checkOffset(offset, byteLength)

        for (i in 0 until min(str.length, byteLength)) {
            val codePoint = str[i].code.toByte()
            buf.put(offset + i, codePoint)
        }

        for (i in str.length until byteLength) {
            buf.put(offset + i, 0)
        }

        return this
    }

    actual fun setStringUtf16(offset: Int, str: String, byteLength: Int): Buffer {
        checkOffset(offset, byteLength)

        for (i in 0 until min(str.length, byteLength / 2)) {
            val codePoint = str[i].code.toShort()
            buf.putShort(offset + 2 * i, codePoint)
        }

        for (i in 2 * str.length until byteLength) {
            buf.putShort(offset + i, 0)
        }

        return this
    }

    actual fun zero(): Buffer =
        fillByte(0)

    actual fun fillByte(value: Byte): Buffer {
        for (i in 0 until size) {
            buf.put(i, value)
        }

        return this
    }

    actual fun toBase64(): String {
        buf.limit(size)
        val str = String(Base64.getEncoder().encode(buf).array())
        buf.position(0)
        buf.limit(capacity)
        return str
    }

    actual fun copy(offset: Int, size: Int): Buffer {
        val newBuffer = withSize(size, endianness)
        copyInto(newBuffer, destinationOffset = 0, offset, size.coerceAtMost(this.size - offset))
        return newBuffer
    }

    actual fun copyInto(destination: Buffer, destinationOffset: Int, offset: Int, size: Int) {
        require(offset >= 0 && offset <= this.size) {
            "Offset $offset is out of bounds."
        }
        require(destinationOffset >= 0 && destinationOffset <= destination.size) {
            "Destination offset $destinationOffset is out of bounds."
        }
        require(
            size >= 0 &&
                    destinationOffset + size <= destination.size &&
                    offset + size <= this.size
        ) {
            "Size $size is out of bounds."
        }

        byteArray.copyInto(
            destination.byteArray,
            destinationOffset,
            startIndex = offset,
            endIndex = offset + size,
        )
    }

    /**
     * Checks whether we can read [size] bytes at [offset].
     */
    private fun checkOffset(offset: Int, size: Int) {
        require(offset >= 0 && offset + size <= this.size) {
            "Offset $offset is out of bounds."
        }
    }

    /**
     * Reallocates the underlying ArrayBuffer if necessary.
     */
    private fun ensureCapacity(minNewSize: Int) {
        if (minNewSize > capacity) {
            var newSize = if (capacity == 0) minNewSize else capacity

            do {
                newSize *= 2
            } while (newSize < minNewSize)

            val newBuf = ByteBuffer.allocate(newSize)
            newBuf.order(buf.order())
            newBuf.put(buf.array())
            buf = newBuf
        }
    }

    actual companion object {
        actual fun withCapacity(
            initialCapacity: Int,
            endianness: Endianness,
        ): Buffer =
            Buffer(ByteBuffer.allocate(initialCapacity), size = 0, endianness)

        actual fun withSize(initialSize: Int, endianness: Endianness): Buffer =
            Buffer(ByteBuffer.allocate(initialSize), initialSize, endianness)

        actual fun fromByteArray(array: ByteArray, endianness: Endianness): Buffer =
            Buffer(ByteBuffer.wrap(array), array.size, endianness)

        actual fun fromBase64(data: String, endianness: Endianness): Buffer =
            fromByteArray(Base64.getDecoder().decode(data), endianness)

        fun fromResource(name: String): Buffer {
            val stream = (Buffer::class.java.getResourceAsStream(name)
                ?: error("""Couldn't load resource "$name"."""))

            return stream.use { fromByteArray(it.readBytes()) }
        }
    }
}
