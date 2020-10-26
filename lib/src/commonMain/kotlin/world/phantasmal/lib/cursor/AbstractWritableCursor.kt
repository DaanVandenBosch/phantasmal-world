package world.phantasmal.lib.cursor

import world.phantasmal.lib.ZERO_U16
import world.phantasmal.lib.ZERO_U8
import kotlin.math.min

abstract class AbstractWritableCursor
protected constructor(protected val offset: Int) : WritableCursor {
    override var position: Int = 0
        protected set

    override val bytesLeft: Int
        get() = size - position

    protected val absolutePosition: Int
        get() = offset + position

    override fun hasBytesLeft(bytes: Int): Boolean =
        bytesLeft >= bytes

    override fun seek(offset: Int): WritableCursor =
        seekStart(position + offset)

    override fun seekStart(offset: Int): WritableCursor {
        require(offset >= 0 || offset <= size) { "Offset $offset is out of bounds." }

        position = offset
        return this
    }

    override fun seekEnd(offset: Int): WritableCursor {
        require(offset >= 0 || offset <= size) { "Offset $offset is out of bounds." }

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
                val codePoint = u8()

                if (nullTerminated && codePoint == ZERO_U8) {
                    if (dropRemaining) {
                        seek(maxByteLength - i - 1)
                    }

                    break
                }

                append(codePoint.toShort().toChar())
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
                val codePoint = u16()

                if (nullTerminated && codePoint == ZERO_U16) {
                    if (dropRemaining) {
                        seek(maxByteLength - 2 * i - 2)
                    }

                    break
                }

                append(codePoint.toShort().toChar())
            }
        }

    override fun writeU8Array(array: UByteArray): WritableCursor {
        val len = array.size
        requireSize(len)

        for (i in 0 until len) {
            writeU8(array[i])
        }

        return this
    }

    override fun writeU16Array(array: UShortArray): WritableCursor {
        val len = array.size
        requireSize(2 * len)

        for (i in 0 until len) {
            writeU16(array[i])
        }

        return this
    }

    override fun writeU32Array(array: UIntArray): WritableCursor {
        val len = array.size
        requireSize(4 * len)

        for (i in 0 until len) {
            writeU32(array[i])
        }

        return this
    }

    override fun writeI32Array(array: IntArray): WritableCursor {
        val len = array.size
        requireSize(4 * len)

        for (i in 0 until len) {
            writeI32(array[i])
        }

        return this
    }

    override fun writeCursor(other: Cursor): WritableCursor {
        val size = other.bytesLeft
        requireSize(size)
        for (i in 0 until size) {
            writeI8(other.i8())
        }

        return this
    }

    override fun writeStringAscii(str: String, byteLength: Int): WritableCursor {
        requireSize(byteLength)

        val len = min(byteLength, str.length)

        for (i in 0 until len) {
            writeI8(str[i].toByte())
        }

        val padLen = byteLength - len

        for (i in 0 until padLen) {
            writeI8(0)
        }

        return this
    }

    override fun writeStringUtf16(str: String, byteLength: Int): WritableCursor {
        requireSize(byteLength)

        val maxLen = byteLength / 2
        val len = min(maxLen, str.length)

        for (i in 0 until len) {
            writeI16(str[i].toShort())
        }

        val padLen = maxLen - len

        for (i in 0 until padLen) {
            writeI16(0)
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
