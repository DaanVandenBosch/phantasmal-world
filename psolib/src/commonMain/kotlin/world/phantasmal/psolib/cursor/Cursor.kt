package world.phantasmal.psolib.cursor

import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.buffer.Buffer

/**
 * A cursor for reading binary data.
 */
interface Cursor {
    val size: Int

    /**
     * The position from where bytes will be read or written.
     */
    val position: Int

    /**
     * Byte order mode.
     */
    var endianness: Endianness

    val bytesLeft: Int

    fun hasBytesLeft(): Boolean

    /**
     * Seek forward or backward by a number of bytes.
     *
     * @param offset if positive, seeks forward by offset bytes, otherwise seeks backward by -offset
     * bytes.
     */
    fun seek(offset: Int): Cursor

    /**
     * Seek forward from the start of the cursor by a number of bytes.
     *
     * @param offset greater or equal to 0 and smaller than size
     */
    fun seekStart(offset: Int): Cursor

    /**
     * Seek backward from the end of the cursor by a number of bytes.
     *
     * @param offset greater or equal to 0 and smaller than size
     */
    fun seekEnd(offset: Int): Cursor

    /**
     * Reads an unsigned 8-bit integer and increments position by 1.
     */
    fun uByte(): UByte

    /**
     * Reads an unsigned 16-bit integer and increments position by 2.
     */
    fun uShort(): UShort

    /**
     * Reads an unsigned 32-bit integer and increments position by 4.
     */
    fun uInt(): UInt

    /**
     * Reads an signed 8-bit integer and increments position by 1.
     */
    fun byte(): Byte

    /**
     * Reads a signed 16-bit integer and increments position by 2.
     */
    fun short(): Short

    /**
     * Reads a signed 32-bit integer and increments position by 4.
     */
    fun int(): Int

    /**
     * Reads a 32-bit floating point number and increments position by 4.
     */
    fun float(): Float

    /**
     * Reads [n] unsigned 8-bit integers and increments position by [n].
     */
    fun uByteArray(n: Int): UByteArray

    /**
     * Reads [n] unsigned 16-bit integers and increments position by 2[n].
     */
    fun uShortArray(n: Int): UShortArray

    /**
     * Reads [n] unsigned 32-bit integers and increments position by 4[n].
     */
    fun uIntArray(n: Int): UIntArray

    /**
     * Reads [n] signed 8-bit integers and increments position by [n].
     */
    fun byteArray(n: Int): ByteArray

    /**
     * Reads [n] signed 32-bit integers and increments position by 4[n].
     */
    fun intArray(n: Int): IntArray

    /**
     * Consumes a variable number of bytes.
     *
     * @param size the amount bytes to consume.
     * @return a view containing size bytes.
     */
    fun take(size: Int): Cursor

    /**
     * Consumes up to [maxByteLength] bytes.
     */
    fun stringAscii(
        maxByteLength: Int,
        nullTerminated: Boolean = true,
        dropRemaining: Boolean = true,
    ): String

    /**
     * Consumes up to [maxByteLength] bytes.
     */
    fun stringUtf16(
        maxByteLength: Int,
        nullTerminated: Boolean = true,
        dropRemaining: Boolean = true,
    ): String

    /**
     * Returns a buffer with a copy of [size] bytes at [position].
     */
    fun buffer(size: Int = bytesLeft): Buffer
}
