package world.phantasmal.psolib.buffer

import world.phantasmal.psolib.Endianness

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
    fun getUByte(offset: Int): UByte

    /**
     * Reads an unsigned 16-bit integer at the given offset.
     */
    fun getUShort(offset: Int): UShort

    /**
     * Reads an unsigned 32-bit integer at the given offset.
     */
    fun getUInt(offset: Int): UInt

    /**
     * Reads a signed 8-bit integer at the given offset.
     */
    fun getByte(offset: Int): Byte

    /**
     * Reads a signed 16-bit integer at the given offset.
     */
    fun getShort(offset: Int): Short

    /**
     * Reads a signed 32-bit integer at the given offset.
     */
    fun getInt(offset: Int): Int

    /**
     * Reads a 32-bit floating point number at the given offset.
     */
    fun getFloat(offset: Int): Float

    /**
     * Reads an ASCII-encoded string at the given offset.
     */
    fun getStringAscii(offset: Int, maxByteLength: Int, nullTerminated: Boolean): String

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
    fun setUByte(offset: Int, value: UByte): Buffer

    /**
     * Writes an unsigned 16-bit integer at the given offset.
     */
    fun setUShort(offset: Int, value: UShort): Buffer

    /**
     * Writes an unsigned 32-bit integer at the given offset.
     */
    fun setUInt(offset: Int, value: UInt): Buffer

    /**
     * Writes a signed 8-bit integer at the given offset.
     */
    fun setByte(offset: Int, value: Byte): Buffer

    /**
     * Writes a signed 16-bit integer at the given offset.
     */
    fun setShort(offset: Int, value: Short): Buffer

    /**
     * Writes a signed 32-bit integer at the given offset.
     */
    fun setInt(offset: Int, value: Int): Buffer

    /**
     * Writes a 32-bit floating point number at the given offset.
     */
    fun setFloat(offset: Int, value: Float): Buffer

    /**
     * Writes an ASCII-encoded string at the given offset. If [str] is shorter than [byteLength],
     * nul bytes will be inserted until [byteLength] bytes have been written.
     */
    fun setStringAscii(offset: Int, str: String, byteLength: Int): Buffer

    /**
     * Writes a UTF-16-encoded string at the given offset. If less than [byteLength] bytes can be
     * written this way, nul bytes will be inserted until [byteLength] bytes have been written.
     */
    fun setStringUtf16(offset: Int, str: String, byteLength: Int): Buffer

    /**
     * Writes 0 bytes to the entire buffer.
     */
    fun zero(): Buffer

    /**
     * Writes [value] to every byte in the buffer.
     */
    fun fillByte(value: Byte): Buffer

    fun toBase64(): String

    /**
     * Returns a copy of this buffer. The copy's capacity will equal its size. If [size] is greater
     * than this buffer's size, the remaining bytes will be zeroed out.
     */
    fun copy(offset: Int = 0, size: Int = this.size): Buffer

    fun copyInto(
        destination: Buffer,
        destinationOffset: Int = 0,
        offset: Int = 0,
        size: Int = this.size,
    )

    companion object {
        /**
         * Returns a new buffer the given initial capacity and size 0.
         */
        fun withCapacity(initialCapacity: Int, endianness: Endianness = Endianness.Little): Buffer

        /**
         * Returns a new buffer with an initial size and capacity of [initialSize].
         */
        fun withSize(initialSize: Int, endianness: Endianness = Endianness.Little): Buffer

        fun fromByteArray(array: ByteArray, endianness: Endianness = Endianness.Little): Buffer

        fun fromBase64(data: String, endianness: Endianness = Endianness.Little): Buffer
    }
}
