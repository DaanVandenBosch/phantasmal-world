package world.phantasmal.lib.buffer

import world.phantasmal.lib.Endianness

/**
 * Resizable, continuous block of bytes which is reallocated when necessary.
 */
expect class Buffer {
    var size: UInt

    /**
     * Byte order mode.
     */
    var endianness: Endianness

    val capacity: UInt

    /**
     * Reads an unsigned 8-bit integer at the given offset.
     */
    fun getU8(offset: UInt): UByte

    /**
     * Reads an unsigned 16-bit integer at the given offset.
     */
    fun getU16(offset: UInt): UShort

    /**
     * Reads an unsigned 32-bit integer at the given offset.
     */
    fun getU32(offset: UInt): UInt

    /**
     * Reads a signed 8-bit integer at the given offset.
     */
    fun getI8(offset: UInt): Byte

    /**
     * Reads a signed 16-bit integer at the given offset.
     */
    fun getI16(offset: UInt): Short

    /**
     * Reads a signed 32-bit integer at the given offset.
     */
    fun getI32(offset: UInt): Int

    /**
     * Reads a 32-bit floating point number at the given offset.
     */
    fun getF32(offset: UInt): Float

    /**
     * Reads a UTF-16-encoded string at the given offset.
     */
    fun getStringUtf16(offset: UInt, maxByteLength: UInt, nullTerminated: Boolean): String

    /**
     * Returns a copy of this buffer at the given offset with the given size.
     */
    fun slice(offset: UInt, size: UInt): Buffer

    /**
     * Writes an unsigned 8-bit integer at the given offset.
     */
    fun setU8(offset: UInt, value: UByte): Buffer

    /**
     * Writes an unsigned 16-bit integer at the given offset.
     */
    fun setU16(offset: UInt, value: UShort): Buffer

    /**
     * Writes an unsigned 32-bit integer at the given offset.
     */
    fun setU32(offset: UInt, value: UInt): Buffer

    /**
     * Writes a signed 8-bit integer at the given offset.
     */
    fun setI8(offset: UInt, value: Byte): Buffer

    /**
     * Writes a signed 16-bit integer at the given offset.
     */
    fun setI16(offset: UInt, value: Short): Buffer

    /**
     * Writes a signed 32-bit integer at the given offset.
     */
    fun setI32(offset: UInt, value: Int): Buffer

    /**
     * Writes a 32-bit floating point number at the given offset.
     */
    fun setF32(offset: UInt, value: Float): Buffer

    /**
     * Writes 0 bytes to the entire buffer.
     */
    fun zero(): Buffer

    companion object {
        fun withCapacity(initialCapacity: UInt, endianness: Endianness = Endianness.Little): Buffer

        fun fromByteArray(array: ByteArray, endianness: Endianness = Endianness.Little): Buffer
    }
}
