package world.phantasmal.lib.cursor

import kotlin.experimental.and
import kotlin.math.min

abstract class AbstractWritableCursor
protected constructor(protected val offset: Int) : WritableCursor {
    override var position: Int = 0
        protected set

    override val bytesLeft: Int
        get() = size - position

    protected val absolutePosition: Int
        get() = offset + position

    override fun hasBytesLeft(atLeast: Int): Boolean =
        bytesLeft >= atLeast

    override fun seek(offset: Int): WritableCursor =
        seekStart(position + offset)

    override fun seekStart(offset: Int): WritableCursor {
        require(offset in 0..size) { "Offset $offset is out of bounds." }

        position = offset
        return this
    }

    override fun seekEnd(offset: Int): WritableCursor {
        require(offset in 0..size) { "Offset $offset is out of bounds." }

        position = size - offset
        return this
    }

    override fun stringAscii(
        maxByteLength: Int,
        nullTerminated: Boolean,
        dropRemaining: Boolean,
    ): String =
        buildString {
            for (i in 0 until maxByteLength) {
                // Use Byte instead of UByte for better KJS perf.
                val codePoint = (byte().toShort() and 0xFF).toChar()

                if (nullTerminated && codePoint == '\u0000') {
                    if (dropRemaining) {
                        seek(maxByteLength - i - 1)
                    }

                    break
                }

                append(codePoint)
            }
        }

    override fun stringUtf16(
        maxByteLength: Int,
        nullTerminated: Boolean,
        dropRemaining: Boolean,
    ): String =
        buildString {
            val len = maxByteLength / 2

            for (i in 0 until len) {
                val codePoint = short().toChar()

                if (nullTerminated && codePoint == '\u0000') {
                    if (dropRemaining) {
                        seek(maxByteLength - 2 * i - 2)
                    }

                    break
                }

                append(codePoint)
            }
        }

    override fun writeUByteArray(array: UByteArray): WritableCursor {
        val len = array.size
        requireSize(len)

        for (i in 0 until len) {
            writeUByte(array[i])
        }

        return this
    }

    override fun writeUShortArray(array: UShortArray): WritableCursor {
        val len = array.size
        requireSize(2 * len)

        for (i in 0 until len) {
            writeUShort(array[i])
        }

        return this
    }

    override fun writeUIntArray(array: UIntArray): WritableCursor {
        val len = array.size
        requireSize(4 * len)

        for (i in 0 until len) {
            writeUInt(array[i])
        }

        return this
    }

    override fun writeByteArray(array: ByteArray): WritableCursor {
        val len = array.size
        requireSize(len)

        for (i in 0 until len) {
            writeByte(array[i])
        }

        return this
    }

    override fun writeIntArray(array: IntArray): WritableCursor {
        val len = array.size
        requireSize(4 * len)

        for (i in 0 until len) {
            writeInt(array[i])
        }

        return this
    }

    override fun writeCursor(other: Cursor): WritableCursor {
        val size = other.bytesLeft
        requireSize(size)

        for (i in 0 until size) {
            writeByte(other.byte())
        }

        return this
    }

    override fun writeStringAscii(str: String, byteLength: Int): WritableCursor {
        requireSize(byteLength)

        val len = min(byteLength, str.length)

        for (i in 0 until len) {
            writeByte(str[i].toByte())
        }

        val padLen = byteLength - len

        for (i in 0 until padLen) {
            writeByte(0)
        }

        return this
    }

    override fun writeStringUtf16(str: String, byteLength: Int): WritableCursor {
        requireSize(byteLength)

        val maxLen = byteLength / 2
        val len = min(maxLen, str.length)

        for (i in 0 until len) {
            writeShort(str[i].toShort())
        }

        val padLen = maxLen - len

        for (i in 0 until padLen) {
            writeShort(0)
        }

        return this
    }

    /**
     * Throws an error if less than [size] bytes are left at [position].
     */
    protected fun requireSize(size: Int) {
        val left = this.size - position

        require(size <= left) { "$size Bytes required but only $left available." }
    }
}
