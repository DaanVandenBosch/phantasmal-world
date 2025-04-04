package world.phantasmal.psolib.buffer

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.DataView
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.w3c.dom.WindowOrWorkerGlobalScope
import world.phantasmal.psolib.Endianness
import kotlin.math.min

external val self: WindowOrWorkerGlobalScope

actual class Buffer private constructor(
    arrayBuffer: ArrayBuffer,
    size: Int,
    endianness: Endianness,
) {
    var arrayBuffer = arrayBuffer
        private set

    private var dataView = DataView(arrayBuffer)
    private var littleEndian = endianness == Endianness.Little

    actual var size: Int = size
        set(value) {
            ensureCapacity(value)
            field = value
        }

    actual var endianness: Endianness
        get() = if (littleEndian) Endianness.Little else Endianness.Big
        set(value) {
            littleEndian = value == Endianness.Little
        }

    actual val capacity: Int
        get() = arrayBuffer.byteLength

    actual fun getUByte(offset: Int): UByte {
        checkOffset(offset, 1)
        return dataView.getUint8(offset).toUByte()
    }

    actual fun getUShort(offset: Int): UShort {
        checkOffset(offset, 2)
        return dataView.getUint16(offset, littleEndian).toUShort()
    }

    actual fun getUInt(offset: Int): UInt {
        checkOffset(offset, 4)
        return dataView.getUint32(offset, littleEndian).toUInt()
    }

    actual fun getByte(offset: Int): Byte {
        checkOffset(offset, 1)
        return dataView.getInt8(offset)
    }

    actual fun getShort(offset: Int): Short {
        checkOffset(offset, 2)
        return dataView.getInt16(offset, littleEndian)
    }

    actual fun getInt(offset: Int): Int {
        checkOffset(offset, 4)
        return dataView.getInt32(offset, littleEndian)
    }

    actual fun getFloat(offset: Int): Float {
        checkOffset(offset, 4)
        return dataView.getFloat32(offset, littleEndian)
    }

    actual fun getStringAscii(
        offset: Int,
        maxByteLength: Int,
        nullTerminated: Boolean,
    ): String =
        buildString {
            for (i in 0 until maxByteLength) {
                val codePoint = (getByte(offset + i).toInt() and 0xFF).toChar()

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
                val codePoint = getShort(offset + i * 2).toInt().toChar()

                if (nullTerminated && codePoint == '0') {
                    break
                }

                append(codePoint)
            }
        }

    actual fun slice(offset: Int, size: Int): Buffer {
        checkOffset(offset, size)
        return fromArrayBuffer(
            arrayBuffer.slice(offset, (offset + size)),
            endianness
        )
    }

    actual fun setUByte(offset: Int, value: UByte): Buffer {
        checkOffset(offset, 1)
        dataView.setUint8(offset, value.toByte())
        return this
    }

    actual fun setUShort(offset: Int, value: UShort): Buffer {
        checkOffset(offset, 2)
        dataView.setUint16(offset, value.toShort(), littleEndian)
        return this
    }

    actual fun setUInt(offset: Int, value: UInt): Buffer {
        checkOffset(offset, 4)
        dataView.setUint32(offset, value.toInt(), littleEndian)
        return this
    }

    actual fun setByte(offset: Int, value: Byte): Buffer {
        checkOffset(offset, 1)
        dataView.setInt8(offset, value)
        return this
    }

    actual fun setShort(offset: Int, value: Short): Buffer {
        checkOffset(offset, 2)
        dataView.setInt16(offset, value, littleEndian)
        return this
    }

    actual fun setInt(offset: Int, value: Int): Buffer {
        checkOffset(offset, 4)
        dataView.setInt32(offset, value, littleEndian)
        return this
    }

    actual fun setFloat(offset: Int, value: Float): Buffer {
        checkOffset(offset, 4)
        dataView.setFloat32(offset, value, littleEndian)
        return this
    }

    actual fun setStringAscii(offset: Int, str: String, byteLength: Int): Buffer {
        checkOffset(offset, byteLength)

        for (i in 0 until min(str.length, byteLength)) {
            val codePoint = str[i].code.toByte()
            dataView.setInt8(offset + i, codePoint)
        }

        for (i in str.length until byteLength) {
            dataView.setInt8(offset + i, 0)
        }

        return this
    }

    actual fun setStringUtf16(offset: Int, str: String, byteLength: Int): Buffer {
        checkOffset(offset, byteLength)

        for (i in 0 until min(str.length, byteLength / 2)) {
            val codePoint = str[i].code.toShort()
            dataView.setInt16(offset + 2 * i, codePoint)
        }

        for (i in 2 * str.length until byteLength) {
            dataView.setInt8(offset + i, 0)
        }

        return this
    }

    actual fun zero(): Buffer =
        fillByte(0)

    actual fun fillByte(value: Byte): Buffer {
        (Int8Array(arrayBuffer, 0, size).asDynamic()).fill(value)
        return this
    }

    actual fun toBase64(): String {
        var str = ""

        for (i in 0 until size) {
            str += (getByte(i).toInt() and 0xff).toChar()
        }

        return self.btoa(str)
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
        require(destinationOffset >= 0 && destinationOffset <= this.size) {
            "Destination offset $destinationOffset is out of bounds."
        }
        require(
            size >= 0 &&
                    destinationOffset + size <= destination.size &&
                    offset + size <= this.size
        ) {
            "Size $size is out of bounds."
        }

        Uint8Array(destination.arrayBuffer, destinationOffset)
            .set(Uint8Array(arrayBuffer, offset, size))
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

            val newBuffer = ArrayBuffer(newSize)
            Uint8Array(newBuffer).set(Uint8Array(arrayBuffer, 0, size))
            arrayBuffer = newBuffer
            dataView = DataView(arrayBuffer)
        }
    }

    actual companion object {
        actual fun withCapacity(
            initialCapacity: Int,
            endianness: Endianness,
        ): Buffer =
            Buffer(ArrayBuffer(initialCapacity), size = 0, endianness)

        actual fun withSize(initialSize: Int, endianness: Endianness): Buffer =
            Buffer(ArrayBuffer(initialSize), initialSize, endianness)

        actual fun fromByteArray(array: ByteArray, endianness: Endianness): Buffer {
            val arrayBuffer = ArrayBuffer(array.size)
            Int8Array(arrayBuffer).set(array.toTypedArray())
            return Buffer(arrayBuffer, array.size, endianness)
        }

        fun fromArrayBuffer(arrayBuffer: ArrayBuffer, endianness: Endianness): Buffer =
            Buffer(arrayBuffer, arrayBuffer.byteLength, endianness)

        actual fun fromBase64(data: String, endianness: Endianness): Buffer {
            val str = self.atob(data)
            val buf = withSize(str.length, endianness)

            for (i in 0 until buf.size) {
                buf.setByte(i, str[i].code.toByte())
            }

            return buf
        }
    }
}
