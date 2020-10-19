package world.phantasmal.lib.buffer

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.DataView
import org.khronos.webgl.Uint8Array
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.ZERO_U16

actual class Buffer private constructor(
    private var arrayBuffer: ArrayBuffer,
    size: UInt,
    endianness: Endianness,
) {
    private var dataView = DataView(arrayBuffer)
    private var littleEndian = endianness == Endianness.Little

    actual var size: UInt = size
        set(value) {
            ensureCapacity(value)
            field = value
        }

    actual var endianness: Endianness
        get() = if (littleEndian) Endianness.Little else Endianness.Big
        set(value) {
            littleEndian = value == Endianness.Little
        }

    actual val capacity: UInt
        get() = arrayBuffer.byteLength.toUInt()

    actual fun getU8(offset: UInt): UByte {
        checkOffset(offset, 1u)
        return dataView.getUint8(offset.toInt()).toUByte()
    }

    actual fun getU16(offset: UInt): UShort {
        checkOffset(offset, 2u)
        return dataView.getUint16(offset.toInt(), littleEndian).toUShort()
    }

    actual fun getU32(offset: UInt): UInt {
        checkOffset(offset, 4u)
        return dataView.getUint32(offset.toInt(), littleEndian).toUInt()
    }

    actual fun getI8(offset: UInt): Byte {
        checkOffset(offset, 1u)
        return dataView.getInt8(offset.toInt())
    }

    actual fun getI16(offset: UInt): Short {
        checkOffset(offset, 2u)
        return dataView.getInt16(offset.toInt(), littleEndian)
    }

    actual fun getI32(offset: UInt): Int {
        checkOffset(offset, 4u)
        return dataView.getInt32(offset.toInt(), littleEndian)
    }

    actual fun getF32(offset: UInt): Float {
        checkOffset(offset, 4u)
        return dataView.getFloat32(offset.toInt(), littleEndian)
    }

    actual fun getStringUtf16(
        offset: UInt,
        maxByteLength: UInt,
        nullTerminated: Boolean,
    ): String =
        buildString {
            val len = maxByteLength / 2u

            for (i in 0u until len) {
                val codePoint = getU16(offset + i * 2u)

                if (nullTerminated && codePoint == ZERO_U16) {
                    break
                }

                append(codePoint.toShort().toChar())
            }
        }

    actual fun slice(offset: UInt, size: UInt): Buffer {
        checkOffset(offset, size)
        return fromArrayBuffer(
            arrayBuffer.slice(offset.toInt(), (offset + size).toInt()),
            endianness
        )
    }

    /**
     * Writes an unsigned 8-bit integer at the given offset.
     */
    actual fun setU8(offset: UInt, value: UByte): Buffer {
        checkOffset(offset, 1u)
        dataView.setUint8(offset.toInt(), value.toByte())
        return this
    }

    /**
     * Writes an unsigned 16-bit integer at the given offset.
     */
    actual fun setU16(offset: UInt, value: UShort): Buffer {
        checkOffset(offset, 2u)
        dataView.setUint16(offset.toInt(), value.toShort(), littleEndian)
        return this
    }

    /**
     * Writes an unsigned 32-bit integer at the given offset.
     */
    actual fun setU32(offset: UInt, value: UInt): Buffer {
        checkOffset(offset, 4u)
        dataView.setUint32(offset.toInt(), value.toInt(), littleEndian)
        return this
    }

    /**
     * Writes a signed 8-bit integer at the given offset.
     */
    actual fun setI8(offset: UInt, value: Byte): Buffer {
        checkOffset(offset, 1u)
        dataView.setInt8(offset.toInt(), value)
        return this
    }

    /**
     * Writes a signed 16-bit integer at the given offset.
     */
    actual fun setI16(offset: UInt, value: Short): Buffer {
        checkOffset(offset, 2u)
        dataView.setInt16(offset.toInt(), value, littleEndian)
        return this
    }

    /**
     * Writes a signed 32-bit integer at the given offset.
     */
    actual fun setI32(offset: UInt, value: Int): Buffer {
        checkOffset(offset, 4u)
        dataView.setInt32(offset.toInt(), value, littleEndian)
        return this
    }

    /**
     * Writes a 32-bit floating point number at the given offset.
     */
    actual fun setF32(offset: UInt, value: Float): Buffer {
        checkOffset(offset, 4u)
        dataView.setFloat32(offset.toInt(), value, littleEndian)
        return this
    }

    /**
     * Writes 0 bytes to the entire buffer.
     */
    actual fun zero(): Buffer {
        (Uint8Array(arrayBuffer).asDynamic()).fill(0)
        return this
    }

    /**
     * Checks whether we can read [size] bytes at [offset].
     */
    private fun checkOffset(offset: UInt, size: UInt) {
        require(offset + size <= this.size) {
            "Offset $offset is out of bounds."
        }
    }

    /**
     * Reallocates the underlying ArrayBuffer if necessary.
     */
    private fun ensureCapacity(minNewSize: UInt) {
        if (minNewSize > capacity) {
            var newSize = if (capacity == 0u) minNewSize else capacity;

            do {
                newSize *= 2u;
            } while (newSize < minNewSize);

            val newBuffer = ArrayBuffer(newSize.toInt());
            Uint8Array(newBuffer).set(Uint8Array(arrayBuffer, 0, size.toInt()));
            arrayBuffer = newBuffer;
            dataView = DataView(arrayBuffer);
        }
    }

    actual companion object {
        actual fun withCapacity(
            initialCapacity: UInt,
            endianness: Endianness,
        ): Buffer =
            Buffer(ArrayBuffer(initialCapacity.toInt()), size = 0u, endianness)

        actual fun fromByteArray(array: ByteArray, endianness: Endianness): Buffer {
            val arrayBuffer = ArrayBuffer(array.size)
            Uint8Array(arrayBuffer).set(array.toTypedArray())
            return Buffer(arrayBuffer, array.size.toUInt(), endianness)
        }

        fun fromArrayBuffer(arrayBuffer: ArrayBuffer, endianness: Endianness): Buffer {
            return Buffer(arrayBuffer, arrayBuffer.byteLength.toUInt(), endianness)
        }
    }
}
