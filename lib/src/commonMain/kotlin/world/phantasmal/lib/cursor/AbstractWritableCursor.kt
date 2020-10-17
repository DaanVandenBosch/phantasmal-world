package world.phantasmal.lib.cursor

import world.phantasmal.lib.ZERO_U16
import world.phantasmal.lib.ZERO_U8
import kotlin.math.min

abstract class AbstractWritableCursor
protected constructor(protected val offset: UInt) : WritableCursor {
    override var position: UInt = 0u
        protected set

    override val bytesLeft: UInt
        get() = size - position

    protected val absolutePosition: UInt
        get() = offset + position

    override fun seek(offset: Int): WritableCursor =
        seekStart((position.toInt() + offset).toUInt())

    override fun seekStart(offset: UInt): WritableCursor {
        require(offset <= size) { "Offset $offset is out of bounds." }

        position = offset
        return this
    }

    override fun seekEnd(offset: UInt): WritableCursor {
        require(offset <= size) { "Offset $offset is out of bounds." }

        position = size - offset
        return this
    }

    override fun stringAscii(
        maxByteLength: UInt,
        nullTerminated: Boolean,
        dropRemaining: Boolean,
    ): String =
        buildString {
            for (i in 0u until maxByteLength) {
                val codePoint = u8()

                if (nullTerminated && codePoint == ZERO_U8) {
                    if (dropRemaining) {
                        seek((maxByteLength - i - 1u).toInt())
                    }

                    break
                }

                append(codePoint.toShort().toChar())
            }
        }

    override fun stringUtf16(
        maxByteLength: UInt,
        nullTerminated: Boolean,
        dropRemaining: Boolean,
    ): String =
        buildString {
            val len = maxByteLength / 2u

            for (i in 0u until len) {
                val codePoint = u16()

                if (nullTerminated && codePoint == ZERO_U16) {
                    if (dropRemaining) {
                        seek((maxByteLength - 2u * i - 2u).toInt())
                    }

                    break
                }

                append(codePoint.toShort().toChar())
            }
        }

    override fun writeU8Array(array: UByteArray): WritableCursor {
        val len = array.size
        requireSize(len.toUInt())

        for (i in 0 until len) {
            writeU8(array[i])
        }

        return this
    }

    override fun writeU16Array(array: UShortArray): WritableCursor {
        val len = array.size
        requireSize(2u * len.toUInt())

        for (i in 0 until len) {
            writeU16(array[i])
        }

        return this
    }

    override fun writeU32Array(array: UIntArray): WritableCursor {
        val len = array.size
        requireSize(4u * len.toUInt())

        for (i in 0 until len) {
            writeU32(array[i])
        }

        return this
    }

    override fun writeI32Array(array: IntArray): WritableCursor {
        val len = array.size
        requireSize(4u * len.toUInt())

        for (i in 0 until len) {
            writeI32(array[i])
        }

        return this
    }

    override fun writeCursor(other: Cursor): WritableCursor {
        val size = other.bytesLeft
        requireSize(size)

        for (i in 0u until (size / 4u)) {
            writeU32(other.u32())
        }

        for (i in 0u until (size % 4u)) {
            writeU8(other.u8())
        }

        position += size
        return this
    }

    override fun writeStringAscii(str: String, byteLength: UInt): WritableCursor {
        requireSize(byteLength)

        val len = min(byteLength.toInt(), str.length)

        for (i in 0 until len) {
            writeU8(str[i].toByte().toUByte())
        }

        val padLen = byteLength.toInt() - len

        for (i in 0 until padLen) {
            writeU8(0u)
        }

        return this
    }

    override fun writeStringUtf16(str: String, byteLength: UInt): WritableCursor {
        requireSize(byteLength)

        val maxLen = byteLength.toInt() / 2
        val len = min(maxLen, str.length)

        for (i in 0 until len) {
            writeU16(str[i].toShort().toUShort())
        }

        val padLen = maxLen - len

        for (i in 0 until padLen) {
            writeU16(0u)
        }

        return this
    }

    /**
     * Throws an error if less than [size] bytes are left at [position].
     */
    protected fun requireSize(size: UInt) {
        val left = this.size - position

        require(size <= left) { "$size Bytes required but only $left available." }
    }
}
