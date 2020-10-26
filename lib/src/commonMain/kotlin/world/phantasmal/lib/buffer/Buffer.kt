package world.phantasmal.lib.buffer

import world.phantasmal.lib.Endianness

/**
 * Resizable, continuous block of bytes which is reallocated when necessary.
 */
expect class Buffer {
    var size: Int

    /**
     * Byte order mode.
     */
    var endianness: Endianness

    val capacity: Int

    /**
     * Reads an unsigned 8-bit integer at the given offset.
     */
    fun getU8(offset: Int): UByte

    /**
     * Reads an unsigned 16-bit integer at the given offset.
     */
    fun getU16(offset: Int): UShort

    /**
     * Reads an unsigned 32-bit integer at the given offset.
     */
    fun getU32(offset: Int): UInt

    /**
     * Reads a signed 8-bit integer at the given offset.
     */
    fun getI8(offset: Int): Byte

    /**
     * Reads a signed 16-bit integer at the given offset.
     */
    fun getI16(offset: Int): Short

    /**
     * Reads a signed 32-bit integer at the given offset.
     */
    fun getI32(offset: Int): Int

    /**
     * Reads a 32-bit floating point number at the given offset.
     */
    fun getF32(offset: Int): Float

    /**
     * Reads a UTF-16-encoded string at the given offset.
     */
    fun getStringUtf16(offset: Int, maxByteLength: Int, nullTerminated: Boolean): String

    /**
     * Returns a copy of this buffer at the given offset with the given size.
     */
    fun slice(offset: Int, size: Int): Buffer

    /**
     * Writes an unsigned 8-bit integer at the given offset.
     */
    fun setU8(offset: Int, value: UByte): Buffer

    /**
     * Writes an unsigned 16-bit integer at the given offset.
     */
    fun setU16(offset: Int, value: UShort): Buffer

    /**
     * Writes an unsigned 32-bit integer at the given offset.
     */
    fun setU32(offset: Int, value: UInt): Buffer

    /**
     * Writes a signed 8-bit integer at the given offset.
     */
    fun setI8(offset: Int, value: Byte): Buffer

    /**
     * Writes a signed 16-bit integer at the given offset.
     */
    fun setI16(offset: Int, value: Short): Buffer

    /**
     * Writes a signed 32-bit integer at the given offset.
     */
    fun setI32(offset: Int, value: Int): Buffer

    /**
     * Writes a 32-bit floating point number at the given offset.
     */
    fun setF32(offset: Int, value: Float): Buffer

    /**
     * Writes 0 bytes to the entire buffer.
     */
    fun zero(): Buffer

    /**
     * Writes [value] to every byte in the buffer.
     */
    fun fill(value: Byte): Buffer

    companion object {
        fun withCapacity(initialCapacity: Int, endianness: Endianness = Endianness.Little): Buffer

        fun withSize(initialSize: Int, endianness: Endianness = Endianness.Little): Buffer

        fun fromByteArray(array: ByteArray, endianness: Endianness = Endianness.Little): Buffer
    }
}
